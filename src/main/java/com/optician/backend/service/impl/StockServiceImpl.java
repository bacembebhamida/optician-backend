package com.optician.backend.service.impl;

import com.optician.backend.dto.*;
import com.optician.backend.exception.InsufficientStockException;
import com.optician.backend.exception.InvalidStockOperationException;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.exception.StockReservationException;
import com.optician.backend.model.*;
import com.optician.backend.model.enums.*;
import com.optician.backend.repository.*;
import com.optician.backend.service.ProductAuditService;
import com.optician.backend.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service Stock.
 *
 * RÈGLE CRITIQUE :
 * Toute modification de Stock.quantity DOIT créer un StockMovement.
 * Le verrou pessimiste (LockModeType.PESSIMISTIC_WRITE) est utilisé
 * pour les opérations de sortie et de réservation afin d'éviter
 * les concurrences négatives (double vente, stock négatif).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final StockReservationRepository reservationRepository;
    private final StockAlertRepository alertRepository;
    private final ProductVariantRepository variantRepository;
    private final StoreRepository storeRepository;
    private final ProductAuditService auditService;

    // =========================================================
    // Lecture
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public StockResponseDto getStockById(Long id) {
        return mapToDto(findStockOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponseDto> getStockByVariantId(Long variantId) {
        return stockRepository.findByProductVariantId(variantId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponseDto> getStockByProductId(Long productId) {
        return stockRepository.findByProductId(productId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponseDto> getStockByStoreId(Long storeId) {
        return stockRepository.findByStoreId(storeId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockResponseDto> getAllStocks(StockSearchFilter filter, Pageable pageable) {
        // Filtre de base : tous les stocks paginés (enrichi si besoin avec Specification)
        Page<Stock> page = stockRepository.findAll(pageable);
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional
    public StockResponseDto getOrCreateStock(Long variantId, Long storeId) {
        return stockRepository.findByProductVariantIdAndStoreId(variantId, storeId)
                .map(this::mapToDto)
                .orElseGet(() -> {
                    ProductVariant variant = variantRepository.findById(variantId)
                            .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
                    Store store = storeRepository.findById(storeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
                    Stock stock = Stock.builder()
                            .productVariant(variant)
                            .store(store)
                            .quantity(0)
                            .reservedQuantity(0)
                            .minimumStock(0)
                            .maximumStock(100)
                            .reorderPoint(5)
                            .build();
                    return mapToDto(stockRepository.save(stock));
                });
    }

    // =========================================================
    // Entrées de stock
    // =========================================================

    @Override
    @Transactional
    public StockMovementResponseDto processEntry(StockEntryRequestDto request) {
        validateEntryType(request.getType());

        // Obtenir ou créer le stock (lecture normale, les entrées n'ont pas de risque de concurrence négative)
        Stock stock = stockRepository.findByProductVariantIdAndStoreId(
                request.getProductVariantId(), request.getStoreId())
                .orElseGet(() -> {
                    ProductVariant variant = variantRepository.findById(request.getProductVariantId())
                            .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", request.getProductVariantId()));
                    Store store = storeRepository.findById(request.getStoreId())
                            .orElseThrow(() -> new ResourceNotFoundException("Store", request.getStoreId()));
                    return stockRepository.save(Stock.builder().productVariant(variant).store(store)
                            .quantity(0).reservedQuantity(0).minimumStock(0).maximumStock(100).reorderPoint(5).build());
                });

        int before = stock.getQuantity();
        int after = before + request.getQuantity();
        stock.setQuantity(after);
        stockRepository.save(stock);

        StockMovement movement = createMovement(stock, request.getType(), request.getQuantity(), before, after,
                request.getReference(), request.getReason());

        checkAndGenerateAlerts(stock.getId());

        auditService.logAudit(AuditAction.STOCK_ENTRY, "Stock", stock.getId(),
                "qty=" + before, "qty=" + after + " | mvt=" + request.getType());

        log.info("Stock entry: variant={} store={} type={} qty={} ({}→{})",
                request.getProductVariantId(), request.getStoreId(), request.getType(), request.getQuantity(), before, after);

        return mapMovementToDto(movement);
    }

    // =========================================================
    // Sorties de stock
    // =========================================================

    @Override
    @Transactional
    public StockMovementResponseDto processExit(StockExitRequestDto request) {
        validateExitType(request.getType());

        // Verrou pessimiste pour éviter le stock négatif en concurrence
        Stock stock = stockRepository.findByVariantAndStoreForUpdate(
                request.getProductVariantId(), request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock pour variante " +
                        request.getProductVariantId() + " dans le magasin " + request.getStoreId()));

        int available = stock.getAvailableQuantity();
        if (available < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Stock insuffisant : disponible=" + available + ", demandé=" + request.getQuantity());
        }

        int before = stock.getQuantity();
        int after = before - request.getQuantity();
        stock.setQuantity(after);
        stockRepository.save(stock);

        StockMovement movement = createMovement(stock, request.getType(), request.getQuantity(), before, after,
                request.getReference(), request.getReason());

        checkAndGenerateAlerts(stock.getId());

        auditService.logAudit(AuditAction.STOCK_EXIT, "Stock", stock.getId(),
                "qty=" + before, "qty=" + after + " | mvt=" + request.getType());

        log.info("Stock exit: variant={} store={} type={} qty={} ({}→{})",
                request.getProductVariantId(), request.getStoreId(), request.getType(), request.getQuantity(), before, after);

        return mapMovementToDto(movement);
    }

    // =========================================================
    // Réservations
    // =========================================================

    @Override
    @Transactional
    public StockReservationResponseDto reserveStock(StockReservationRequestDto request) {
        // Verrou pessimiste
        Stock stock = stockRepository.findByVariantAndStoreForUpdate(
                request.getProductVariantId(), request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock pour variante " +
                        request.getProductVariantId() + " dans le magasin " + request.getStoreId()));

        if (stock.getAvailableQuantity() < request.getQuantity()) {
            throw new StockReservationException(
                    "Stock disponible insuffisant pour la réservation : " + stock.getAvailableQuantity());
        }

        stock.setReservedQuantity(stock.getReservedQuantity() + request.getQuantity());
        stockRepository.save(stock);

        LocalDateTime expiresAt = request.getExpirationMinutes() != null
                ? LocalDateTime.now().plusMinutes(request.getExpirationMinutes())
                : LocalDateTime.now().plusHours(1);

        StockReservation reservation = StockReservation.builder()
                .stock(stock)
                .orderReference(request.getOrderReference())
                .quantity(request.getQuantity())
                .status(ReservationStatus.ACTIVE)
                .expiresAt(expiresAt)
                .user(auditService.getCurrentUsername())
                .build();

        reservation = reservationRepository.save(reservation);

        auditService.logAudit(AuditAction.STOCK_RESERVATION_CREATED, "StockReservation",
                reservation.getId(), null, "order=" + request.getOrderReference() + " qty=" + request.getQuantity());

        return mapReservationToDto(reservation, stock);
    }

    @Override
    @Transactional
    public void releaseReservation(Long reservationId) {
        StockReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("StockReservation", reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidStockOperationException("Seules les réservations ACTIVE peuvent être libérées. Statut actuel : " + reservation.getStatus());
        }

        Stock stock = stockRepository.findByVariantAndStoreForUpdate(
                reservation.getStock().getProductVariant().getId(),
                reservation.getStock().getStore().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", reservation.getStock().getId()));

        int newReserved = Math.max(0, stock.getReservedQuantity() - reservation.getQuantity());
        stock.setReservedQuantity(newReserved);
        stockRepository.save(stock);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        auditService.logAudit(AuditAction.STOCK_RESERVATION_RELEASED, "StockReservation",
                reservationId, "ACTIVE", "RELEASED");
    }

    @Override
    @Transactional
    public void fulfilReservation(Long reservationId) {
        StockReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("StockReservation", reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidStockOperationException("Seules les réservations ACTIVE peuvent être satisfaites.");
        }

        Stock stock = stockRepository.findByVariantAndStoreForUpdate(
                reservation.getStock().getProductVariant().getId(),
                reservation.getStock().getStore().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", reservation.getStock().getId()));

        int before = stock.getQuantity();
        int after = before - reservation.getQuantity();
        int newReserved = Math.max(0, stock.getReservedQuantity() - reservation.getQuantity());

        stock.setQuantity(after);
        stock.setReservedQuantity(newReserved);
        stockRepository.save(stock);

        reservation.setStatus(ReservationStatus.FULFILLED);
        reservationRepository.save(reservation);

        createMovement(stock, StockMovementType.SALE, reservation.getQuantity(), before, after,
                reservation.getOrderReference(), "Satisfaction réservation");

        checkAndGenerateAlerts(stock.getId());
    }

    @Override
    @Transactional
    public void releaseExpiredReservations() {
        List<StockReservation> expired = reservationRepository.findExpiredReservations(LocalDateTime.now());
        for (StockReservation r : expired) {
            try {
                releaseReservation(r.getId());
                log.info("Réservation expirée libérée: id={} order={}", r.getId(), r.getOrderReference());
            } catch (Exception e) {
                log.error("Erreur libération réservation expirée id={}: {}", r.getId(), e.getMessage());
            }
        }
    }

    // =========================================================
    // Alertes
    // =========================================================

    @Override
    @Transactional
    public void checkAndGenerateAlerts(Long stockId) {
        Stock stock = findStockOrThrow(stockId);
        int qty = stock.getQuantity();
        int reorderPoint = stock.getReorderPoint() != null ? stock.getReorderPoint() : 5;
        int maxStock = stock.getMaximumStock() != null ? stock.getMaximumStock() : Integer.MAX_VALUE;

        // OUT_OF_STOCK
        if (qty == 0) {
            generateAlertIfAbsent(stock, StockAlertType.OUT_OF_STOCK);
            resolveAlertIfPresent(stock, StockAlertType.LOW_STOCK);
        }
        // LOW_STOCK
        else if (qty <= reorderPoint) {
            generateAlertIfAbsent(stock, StockAlertType.LOW_STOCK);
            resolveAlertIfPresent(stock, StockAlertType.OUT_OF_STOCK);
        }
        // OVER_STOCK
        else if (qty > maxStock) {
            generateAlertIfAbsent(stock, StockAlertType.OVER_STOCK);
            resolveAlertIfPresent(stock, StockAlertType.LOW_STOCK);
            resolveAlertIfPresent(stock, StockAlertType.OUT_OF_STOCK);
        }
        // Normal
        else {
            resolveAlertIfPresent(stock, StockAlertType.LOW_STOCK);
            resolveAlertIfPresent(stock, StockAlertType.OUT_OF_STOCK);
            resolveAlertIfPresent(stock, StockAlertType.OVER_STOCK);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockAlertResponseDto> getAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable).map(this::mapAlertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockAlertResponseDto> getAlertsByStore(Long storeId, Pageable pageable) {
        return alertRepository.findOpenAlertsByStore(storeId, pageable).map(this::mapAlertToDto);
    }

    @Override
    @Transactional
    public void resolveAlert(Long alertId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("StockAlert", alertId));
        alert.setStatus(StockAlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alertRepository.save(alert);
    }

    // =========================================================
    // Historique des mouvements
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponseDto> getMovements(StockMovementSearchFilter filter, Pageable pageable) {
        return movementRepository.findAll(pageable).map(this::mapMovementToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponseDto> getMovementsByVariant(Long variantId, Pageable pageable) {
        return movementRepository.findByProductVariantId(variantId, pageable).map(this::mapMovementToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponseDto> getMovementsByStore(Long storeId, Pageable pageable) {
        return movementRepository.findByStoreId(storeId, pageable).map(this::mapMovementToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponseDto> getMovementsByProduct(Long productId, Pageable pageable) {
        return movementRepository.findByProductId(productId, pageable).map(this::mapMovementToDto);
    }

    // =========================================================
    // Seuils
    // =========================================================

    @Override
    @Transactional
    public StockResponseDto updateThresholds(Long stockId, Integer minimumStock, Integer maximumStock, Integer reorderPoint) {
        Stock stock = findStockOrThrow(stockId);
        String oldValues = "min=" + stock.getMinimumStock() + " max=" + stock.getMaximumStock() + " reorder=" + stock.getReorderPoint();

        if (minimumStock != null) stock.setMinimumStock(minimumStock);
        if (maximumStock != null) stock.setMaximumStock(maximumStock);
        if (reorderPoint != null) stock.setReorderPoint(reorderPoint);

        stock = stockRepository.save(stock);

        auditService.logAudit(AuditAction.STOCK_THRESHOLD_CHANGE, "Stock", stockId,
                oldValues, "min=" + stock.getMinimumStock() + " max=" + stock.getMaximumStock() + " reorder=" + stock.getReorderPoint());

        checkAndGenerateAlerts(stockId);
        return mapToDto(stock);
    }

    // =========================================================
    // Méthodes privées utilitaires
    // =========================================================

    private Stock findStockOrThrow(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", id));
    }

    private StockMovement createMovement(Stock stock, StockMovementType type, int quantity, int before, int after,
                                          String reference, String reason) {
        StockMovement movement = StockMovement.builder()
                .productVariant(stock.getProductVariant())
                .store(stock.getStore())
                .type(type)
                .quantity(quantity)
                .quantityBefore(before)
                .quantityAfter(after)
                .reference(reference)
                .reason(reason)
                .user(auditService.getCurrentUsername())
                .build();
        return movementRepository.save(movement);
    }

    private void generateAlertIfAbsent(Stock stock, StockAlertType type) {
        boolean exists = alertRepository.findOpenAlertForStockAndType(stock.getId(), type).isPresent();
        if (!exists) {
            StockAlert alert = StockAlert.builder()
                    .stock(stock)
                    .alertType(type)
                    .status(StockAlertStatus.ACTIVE)
                    .build();
            alertRepository.save(alert);
            log.warn("Alerte stock générée: type={} variant={} store={}",
                    type, stock.getProductVariant().getSku(), stock.getStore().getName());
        }
    }

    private void resolveAlertIfPresent(Stock stock, StockAlertType type) {
        alertRepository.findOpenAlertForStockAndType(stock.getId(), type).ifPresent(alert -> {
            alert.setStatus(StockAlertStatus.RESOLVED);
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
        });
    }

    private void validateEntryType(StockMovementType type) {
        if (type != StockMovementType.PURCHASE && type != StockMovementType.RETURN
                && type != StockMovementType.ADJUSTMENT_IN && type != StockMovementType.TRANSFER_IN) {
            throw new InvalidStockOperationException("Type de mouvement invalide pour une entrée : " + type);
        }
    }

    private void validateExitType(StockMovementType type) {
        if (type != StockMovementType.SALE && type != StockMovementType.DAMAGE
                && type != StockMovementType.LOSS && type != StockMovementType.ADJUSTMENT_OUT
                && type != StockMovementType.TRANSFER_OUT) {
            throw new InvalidStockOperationException("Type de mouvement invalide pour une sortie : " + type);
        }
    }

    // =========================================================
    // Mappers
    // =========================================================

    private StockResponseDto mapToDto(Stock s) {
        ProductVariant v = s.getProductVariant();
        Product p = v.getProduct();
        Store store = s.getStore();
        int qty = s.getQuantity() != null ? s.getQuantity() : 0;
        int reserved = s.getReservedQuantity() != null ? s.getReservedQuantity() : 0;
        int reorder = s.getReorderPoint() != null ? s.getReorderPoint() : 5;
        int max = s.getMaximumStock() != null ? s.getMaximumStock() : 100;

        return StockResponseDto.builder()
                .id(s.getId())
                .productVariantId(v.getId())
                .variantSku(v.getSku())
                .variantColor(v.getColor())
                .variantSize(v.getSize())
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getName() : null)
                .brandName(p != null && p.getBrandEntity() != null ? p.getBrandEntity().getName() : null)
                .categoryName(p != null && p.getCategoryEntity() != null ? p.getCategoryEntity().getName() : null)
                .storeId(store.getId())
                .storeName(store.getName())
                .storeCity(store.getCity())
                .quantity(qty)
                .reservedQuantity(reserved)
                .availableQuantity(s.getAvailableQuantity())
                .minimumStock(s.getMinimumStock())
                .maximumStock(max)
                .reorderPoint(reorder)
                .isLowStock(qty > 0 && qty <= reorder)
                .isOutOfStock(qty == 0)
                .isOverStock(qty > max)
                .version(s.getVersion())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private StockMovementResponseDto mapMovementToDto(StockMovement m) {
        ProductVariant v = m.getProductVariant();
        Product p = v.getProduct();
        return StockMovementResponseDto.builder()
                .id(m.getId())
                .productVariantId(v.getId())
                .variantSku(v.getSku())
                .productName(p != null ? p.getName() : null)
                .storeId(m.getStore().getId())
                .storeName(m.getStore().getName())
                .type(m.getType())
                .quantity(m.getQuantity())
                .quantityBefore(m.getQuantityBefore())
                .quantityAfter(m.getQuantityAfter())
                .reference(m.getReference())
                .reason(m.getReason())
                .user(m.getUser())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private StockReservationResponseDto mapReservationToDto(StockReservation r, Stock stock) {
        return StockReservationResponseDto.builder()
                .id(r.getId())
                .stockId(stock.getId())
                .productVariantId(stock.getProductVariant().getId())
                .variantSku(stock.getProductVariant().getSku())
                .storeId(stock.getStore().getId())
                .storeName(stock.getStore().getName())
                .orderReference(r.getOrderReference())
                .quantity(r.getQuantity())
                .status(r.getStatus())
                .expiresAt(r.getExpiresAt())
                .user(r.getUser())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private StockAlertResponseDto mapAlertToDto(StockAlert a) {
        Stock stock = a.getStock();
        ProductVariant v = stock.getProductVariant();
        Product p = v.getProduct();
        return StockAlertResponseDto.builder()
                .id(a.getId())
                .stockId(stock.getId())
                .productVariantId(v.getId())
                .variantSku(v.getSku())
                .productName(p != null ? p.getName() : null)
                .storeId(stock.getStore().getId())
                .storeName(stock.getStore().getName())
                .currentQuantity(stock.getQuantity())
                .reorderPoint(stock.getReorderPoint())
                .alertType(a.getAlertType())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .resolvedAt(a.getResolvedAt())
                .build();
    }
}
