package com.optician.backend.service.impl;

import com.optician.backend.dto.StockInventoryRequestDto;
import com.optician.backend.dto.StockInventoryResponseDto;
import com.optician.backend.exception.InvalidStockOperationException;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.model.*;
import com.optician.backend.model.enums.AuditAction;
import com.optician.backend.model.enums.InventoryStatus;
import com.optician.backend.model.enums.StockMovementType;
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
 * Service d'inventaire physique.
 *
 * Workflow :
 *   DRAFT → IN_PROGRESS → COMPLETED (avec corrections stock)
 *                       → CANCELLED
 *
 * À la validation (COMPLETED) :
 *   Pour chaque article avec discordance ≠ 0 :
 *     → INVENTORY_CORRECTION StockMovement
 *     → Mise à jour directe de Stock.quantity
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockInventoryServiceImpl {

    private final StockInventoryRepository inventoryRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final ProductVariantRepository variantRepository;
    private final StoreRepository storeRepository;
    private final ProductAuditService auditService;

    @Transactional
    public StockInventoryResponseDto createInventory(StockInventoryRequestDto request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", request.getStoreId()));

        String ref = generateInventoryReference();
        StockInventory inventory = StockInventory.builder()
                .inventoryReference(ref)
                .store(store)
                .type(request.getType())
                .status(InventoryStatus.IN_PROGRESS)
                .performedBy(auditService.getCurrentUsername())
                .notes(request.getNotes())
                .build();

        for (StockInventoryRequestDto.InventoryItemDto itemDto : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemDto.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", itemDto.getProductVariantId()));

            // Quantité théorique = stock actuel en base
            int theoretical = stockRepository.findByProductVariantIdAndStoreId(variant.getId(), store.getId())
                    .map(s -> s.getQuantity() != null ? s.getQuantity() : 0)
                    .orElse(0);

            int physical = itemDto.getPhysicalQuantity();
            int discrepancy = physical - theoretical;

            StockInventoryItem item = StockInventoryItem.builder()
                    .stockInventory(inventory)
                    .productVariant(variant)
                    .theoreticalQuantity(theoretical)
                    .physicalQuantity(physical)
                    .discrepancyQuantity(discrepancy)
                    .adjusted(false)
                    .notes(itemDto.getNotes())
                    .build();
            inventory.getItems().add(item);
        }

        inventory = inventoryRepository.save(inventory);

        auditService.logAudit(AuditAction.STOCK_INVENTORY_CREATED, "StockInventory", inventory.getId(),
                null, "ref=" + ref + " store=" + store.getName() + " items=" + request.getItems().size());

        log.info("Inventaire créé: ref={} store={} items={}", ref, store.getName(), request.getItems().size());
        return mapToDto(inventory);
    }

    @Transactional
    public StockInventoryResponseDto validateInventory(Long id) {
        StockInventory inventory = findOrThrow(id);
        if (inventory.getStatus() != InventoryStatus.IN_PROGRESS) {
            throw new InvalidStockOperationException(
                    "Seul un inventaire IN_PROGRESS peut être validé. Statut actuel : " + inventory.getStatus());
        }

        final Store store = inventory.getStore();
        int corrections = 0;
        for (StockInventoryItem item : inventory.getItems()) {
            if (item.getDiscrepancyQuantity() != 0) {
                // Appliquer la correction de stock
                Stock stock = stockRepository.findByProductVariantIdAndStoreId(
                        item.getProductVariant().getId(), store.getId())
                        .orElseGet(() -> stockRepository.save(Stock.builder()
                                .productVariant(item.getProductVariant())
                                .store(store)
                                .quantity(0).reservedQuantity(0).minimumStock(0).maximumStock(100).reorderPoint(5)
                                .build()));

                int before = stock.getQuantity();
                int after = item.getPhysicalQuantity();
                stock.setQuantity(after);
                stockRepository.save(stock);

                // Créer StockMovement INVENTORY_CORRECTION
                movementRepository.save(StockMovement.builder()
                        .productVariant(item.getProductVariant())
                        .store(store)
                        .type(StockMovementType.INVENTORY_CORRECTION)
                        .quantity(Math.abs(item.getDiscrepancyQuantity()))
                        .quantityBefore(before)
                        .quantityAfter(after)
                        .reference(inventory.getInventoryReference())
                        .reason("Correction inventaire physique : écart=" + item.getDiscrepancyQuantity())
                        .user(auditService.getCurrentUsername())
                        .build());

                item.setAdjusted(true);
                corrections++;
            }
        }

        inventory.setStatus(InventoryStatus.COMPLETED);
        inventory.setValidatedBy(auditService.getCurrentUsername());
        inventory.setCompletedAt(LocalDateTime.now());
        StockInventory completedInventory = inventoryRepository.save(inventory);

        auditService.logAudit(AuditAction.STOCK_INVENTORY_VALIDATED, "StockInventory", id,
                "IN_PROGRESS", "COMPLETED corrections=" + corrections);

        log.info("Inventaire validé: ref={} corrections={}", completedInventory.getInventoryReference(), corrections);
        return mapToDto(completedInventory);
    }

    @Transactional
    public StockInventoryResponseDto cancelInventory(Long id) {
        StockInventory inventory = findOrThrow(id);
        if (inventory.getStatus() == InventoryStatus.COMPLETED) {
            throw new InvalidStockOperationException("Un inventaire déjà complété ne peut pas être annulé.");
        }
        inventory.setStatus(InventoryStatus.CANCELLED);
        return mapToDto(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public StockInventoryResponseDto getById(Long id) {
        return mapToDto(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<StockInventoryResponseDto> getAll(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<StockInventoryResponseDto> getByStore(Long storeId, Pageable pageable) {
        return inventoryRepository.findByStoreId(storeId, pageable).map(this::mapToDto);
    }

    // =========================================================
    private StockInventory findOrThrow(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockInventory", id));
    }

    private String generateInventoryReference() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "INV-" + date + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private StockInventoryResponseDto mapToDto(StockInventory inv) {
        List<StockInventoryResponseDto.ItemDto> items = inv.getItems().stream().map(item -> {
            ProductVariant v = item.getProductVariant();
            Product p = v != null ? v.getProduct() : null;
            return StockInventoryResponseDto.ItemDto.builder()
                    .id(item.getId())
                    .productVariantId(v != null ? v.getId() : null)
                    .variantSku(v != null ? v.getSku() : null)
                    .productName(p != null ? p.getName() : null)
                    .theoreticalQuantity(item.getTheoreticalQuantity())
                    .physicalQuantity(item.getPhysicalQuantity())
                    .discrepancyQuantity(item.getDiscrepancyQuantity())
                    .adjusted(item.getAdjusted())
                    .notes(item.getNotes())
                    .build();
        }).collect(Collectors.toList());

        int totalDiscrepancy = inv.getItems().stream()
                .mapToInt(StockInventoryItem::getDiscrepancyQuantity)
                .sum();

        return StockInventoryResponseDto.builder()
                .id(inv.getId())
                .inventoryReference(inv.getInventoryReference())
                .storeId(inv.getStore().getId())
                .storeName(inv.getStore().getName())
                .type(inv.getType())
                .status(inv.getStatus())
                .performedBy(inv.getPerformedBy())
                .validatedBy(inv.getValidatedBy())
                .notes(inv.getNotes())
                .items(items)
                .totalItems(items.size())
                .totalDiscrepancy(totalDiscrepancy)
                .createdAt(inv.getCreatedAt())
                .completedAt(inv.getCompletedAt())
                .build();
    }
}
