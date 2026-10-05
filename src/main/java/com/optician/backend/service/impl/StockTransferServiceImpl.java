package com.optician.backend.service.impl;

import com.optician.backend.dto.StockTransferRequestDto;
import com.optician.backend.dto.StockTransferResponseDto;
import com.optician.backend.exception.InsufficientStockException;
import com.optician.backend.exception.InvalidStockOperationException;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.model.*;
import com.optician.backend.model.enums.AuditAction;
import com.optician.backend.model.enums.StockMovementType;
import com.optician.backend.model.enums.TransferStatus;
import com.optician.backend.repository.*;
import com.optician.backend.service.ProductAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des transferts de stock inter-magasins.
 *
 * Workflow :
 *   DRAFT → APPROVED → IN_TRANSIT → RECEIVED
 *                   → CANCELLED (à tout moment avant RECEIVED)
 *
 * Lors de l'expédition (IN_TRANSIT) : TRANSFER_OUT du magasin source.
 * Lors de la réception (RECEIVED)   : TRANSFER_IN  du magasin cible.
 * Aucune modification directe de stock sans StockMovement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockTransferServiceImpl {

    private final StockTransferRepository transferRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final ProductVariantRepository variantRepository;
    private final StoreRepository storeRepository;
    private final ProductAuditService auditService;

    @Transactional
    public StockTransferResponseDto createTransfer(StockTransferRequestDto request) {
        if (request.getSourceStoreId().equals(request.getTargetStoreId())) {
            throw new InvalidStockOperationException("Le magasin source et le magasin cible doivent être différents.");
        }

        Store source = storeRepository.findById(request.getSourceStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", request.getSourceStoreId()));
        Store target = storeRepository.findById(request.getTargetStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", request.getTargetStoreId()));

        String ref = generateTransferReference();
        StockTransfer transfer = StockTransfer.builder()
                .transferReference(ref)
                .sourceStore(source)
                .targetStore(target)
                .status(TransferStatus.DRAFT)
                .createdBy(auditService.getCurrentUsername())
                .notes(request.getNotes())
                .build();

        for (StockTransferRequestDto.StockTransferItemDto itemDto : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemDto.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", itemDto.getProductVariantId()));
            StockTransferItem item = StockTransferItem.builder()
                    .stockTransfer(transfer)
                    .productVariant(variant)
                    .quantityRequested(itemDto.getQuantityRequested())
                    .quantityShipped(0)
                    .quantityReceived(0)
                    .build();
            transfer.getItems().add(item);
        }

        transfer = transferRepository.save(transfer);
        auditService.logAudit(AuditAction.STOCK_TRANSFER_CREATED, "StockTransfer", transfer.getId(),
                null, "ref=" + ref + " source=" + source.getName() + " → target=" + target.getName());

        log.info("Transfert créé: ref={} {} → {}", ref, source.getName(), target.getName());
        return mapToDto(transfer);
    }

    @Transactional
    public StockTransferResponseDto approveTransfer(Long id) {
        StockTransfer transfer = findOrThrow(id);
        requireStatus(transfer, TransferStatus.DRAFT, "approuver");

        // Vérifier disponibilité stock source
        for (StockTransferItem item : transfer.getItems()) {
            Stock stock = stockRepository.findByProductVariantIdAndStoreId(
                    item.getProductVariant().getId(), transfer.getSourceStore().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Stock pour variante " + item.getProductVariant().getSku() + " dans le magasin source"));
            if (stock.getAvailableQuantity() < item.getQuantityRequested()) {
                throw new InsufficientStockException(
                        "Stock insuffisant pour " + item.getProductVariant().getSku() +
                        ": disponible=" + stock.getAvailableQuantity() + " demandé=" + item.getQuantityRequested());
            }
        }

        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(auditService.getCurrentUsername());
        transfer = transferRepository.save(transfer);

        auditService.logAudit(AuditAction.STOCK_TRANSFER_APPROVED, "StockTransfer", id,
                "DRAFT", "APPROVED");
        return mapToDto(transfer);
    }

    @Transactional
    public StockTransferResponseDto shipTransfer(Long id) {
        StockTransfer transfer = findOrThrow(id);
        requireStatus(transfer, TransferStatus.APPROVED, "expédier");

        // Créer TRANSFER_OUT pour chaque article depuis le magasin source (verrou pessimiste)
        for (StockTransferItem item : transfer.getItems()) {
            Stock sourceStock = stockRepository.findByVariantAndStoreForUpdate(
                    item.getProductVariant().getId(), transfer.getSourceStore().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stock source pour variante " + item.getProductVariant().getSku()));

            if (sourceStock.getAvailableQuantity() < item.getQuantityRequested()) {
                throw new InsufficientStockException("Stock source insuffisant lors de l'expédition pour " + item.getProductVariant().getSku());
            }

            int before = sourceStock.getQuantity();
            int after = before - item.getQuantityRequested();
            sourceStock.setQuantity(after);
            stockRepository.save(sourceStock);

            movementRepository.save(StockMovement.builder()
                    .productVariant(item.getProductVariant())
                    .store(transfer.getSourceStore())
                    .type(StockMovementType.TRANSFER_OUT)
                    .quantity(item.getQuantityRequested())
                    .quantityBefore(before)
                    .quantityAfter(after)
                    .reference(transfer.getTransferReference())
                    .reason("Transfert vers " + transfer.getTargetStore().getName())
                    .user(auditService.getCurrentUsername())
                    .build());

            item.setQuantityShipped(item.getQuantityRequested());
        }

        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer = transferRepository.save(transfer);
        log.info("Transfert expédié: ref={}", transfer.getTransferReference());
        return mapToDto(transfer);
    }

    @Transactional
    public StockTransferResponseDto receiveTransfer(Long id) {
        StockTransfer transfer = findOrThrow(id);
        requireStatus(transfer, TransferStatus.IN_TRANSIT, "réceptionner");

        final Store targetStore = transfer.getTargetStore();
        final String transferRef = transfer.getTransferReference();
        final Store sourceStore = transfer.getSourceStore();

        // Créer TRANSFER_IN pour chaque article dans le magasin cible
        for (StockTransferItem item : transfer.getItems()) {
            Stock targetStock = stockRepository.findByProductVariantIdAndStoreId(
                    item.getProductVariant().getId(), targetStore.getId())
                    .orElseGet(() -> stockRepository.save(Stock.builder()
                            .productVariant(item.getProductVariant())
                            .store(targetStore)
                            .quantity(0).reservedQuantity(0).minimumStock(0).maximumStock(100).reorderPoint(5)
                            .build()));

            int before = targetStock.getQuantity();
            int after = before + item.getQuantityShipped();
            targetStock.setQuantity(after);
            stockRepository.save(targetStock);

            movementRepository.save(StockMovement.builder()
                    .productVariant(item.getProductVariant())
                    .store(targetStore)
                    .type(StockMovementType.TRANSFER_IN)
                    .quantity(item.getQuantityShipped())
                    .quantityBefore(before)
                    .quantityAfter(after)
                    .reference(transferRef)
                    .reason("Réception depuis " + sourceStore.getName())
                    .user(auditService.getCurrentUsername())
                    .build());

            item.setQuantityReceived(item.getQuantityShipped());
        }

        transfer.setStatus(TransferStatus.RECEIVED);
        transfer.setReceivedBy(auditService.getCurrentUsername());
        StockTransfer savedTransfer = transferRepository.save(transfer);

        auditService.logAudit(AuditAction.STOCK_TRANSFER_RECEIVED, "StockTransfer", id,
                "IN_TRANSIT", "RECEIVED");
        log.info("Transfert réceptionné: ref={}", savedTransfer.getTransferReference());
        return mapToDto(savedTransfer);
    }

    @Transactional
    public StockTransferResponseDto cancelTransfer(Long id) {
        StockTransfer transfer = findOrThrow(id);
        if (transfer.getStatus() == TransferStatus.RECEIVED) {
            throw new InvalidStockOperationException("Un transfert déjà réceptionné ne peut pas être annulé.");
        }
        final Store srcStore = transfer.getSourceStore();
        final String trfRef = transfer.getTransferReference();

        if (transfer.getStatus() == TransferStatus.IN_TRANSIT) {
            // Réintégrer les quantités dans le stock source
            for (StockTransferItem item : transfer.getItems()) {
                stockRepository.findByProductVariantIdAndStoreId(
                        item.getProductVariant().getId(), srcStore.getId())
                        .ifPresent(sourceStock -> {
                            int before = sourceStock.getQuantity();
                            int after = before + item.getQuantityShipped();
                            sourceStock.setQuantity(after);
                            stockRepository.save(sourceStock);
                            movementRepository.save(StockMovement.builder()
                                    .productVariant(item.getProductVariant())
                                    .store(srcStore)
                                    .type(StockMovementType.ADJUSTMENT_IN)
                                    .quantity(item.getQuantityShipped())
                                    .quantityBefore(before).quantityAfter(after)
                                    .reference(trfRef)
                                    .reason("Annulation transfert IN_TRANSIT → stock réintégré")
                                    .user(auditService.getCurrentUsername())
                                    .build());
                        });
            }
        }
        transfer.setStatus(TransferStatus.CANCELLED);
        StockTransfer cancelledTransfer = transferRepository.save(transfer);
        auditService.logAudit(AuditAction.STOCK_TRANSFER_CANCELLED, "StockTransfer", id, null, "CANCELLED");
        return mapToDto(cancelledTransfer);
    }

    @Transactional(readOnly = true)
    public StockTransferResponseDto getById(Long id) {
        return mapToDto(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<StockTransferResponseDto> getAll(Pageable pageable) {
        return transferRepository.findAll(pageable).map(this::mapToDto);
    }

    // =========================================================
    private StockTransfer findOrThrow(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransfer", id));
    }

    private void requireStatus(StockTransfer transfer, TransferStatus required, String operation) {
        if (transfer.getStatus() != required) {
            throw new InvalidStockOperationException(
                    "Impossible d'" + operation + " un transfert au statut " + transfer.getStatus() +
                    ". Statut requis : " + required);
        }
    }

    private String generateTransferReference() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "TRF-" + date + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private StockTransferResponseDto mapToDto(StockTransfer t) {
        List<StockTransferResponseDto.ItemDto> items = t.getItems().stream().map(item -> {
            ProductVariant v = item.getProductVariant();
            Product p = v != null ? v.getProduct() : null;
            return StockTransferResponseDto.ItemDto.builder()
                    .id(item.getId())
                    .productVariantId(v != null ? v.getId() : null)
                    .variantSku(v != null ? v.getSku() : null)
                    .productName(p != null ? p.getName() : null)
                    .quantityRequested(item.getQuantityRequested())
                    .quantityShipped(item.getQuantityShipped())
                    .quantityReceived(item.getQuantityReceived())
                    .build();
        }).collect(Collectors.toList());

        return StockTransferResponseDto.builder()
                .id(t.getId())
                .transferReference(t.getTransferReference())
                .sourceStoreId(t.getSourceStore().getId())
                .sourceStoreName(t.getSourceStore().getName())
                .targetStoreId(t.getTargetStore().getId())
                .targetStoreName(t.getTargetStore().getName())
                .status(t.getStatus())
                .createdBy(t.getCreatedBy())
                .approvedBy(t.getApprovedBy())
                .receivedBy(t.getReceivedBy())
                .notes(t.getNotes())
                .items(items)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
