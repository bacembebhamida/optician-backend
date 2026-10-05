package com.optician.backend.service;

import com.optician.backend.dto.*;
import com.optician.backend.model.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface principale du service de gestion du stock.
 *
 * RÈGLE CRITIQUE : Aucune modification de quantité ne peut être effectuée
 * directement. Toute modification DOIT passer par ce service qui garantit
 * la création d'un StockMovement et le maintien de la cohérence.
 */
public interface StockService {

    // =========================================================
    // Lecture du stock
    // =========================================================

    StockResponseDto getStockById(Long id);

    List<StockResponseDto> getStockByVariantId(Long variantId);

    List<StockResponseDto> getStockByProductId(Long productId);

    List<StockResponseDto> getStockByStoreId(Long storeId);

    Page<StockResponseDto> getAllStocks(StockSearchFilter filter, Pageable pageable);

    /**
     * Obtenir ou créer le stock pour une variante et un magasin.
     * Si l'enregistrement n'existe pas, il est créé avec quantité = 0.
     */
    StockResponseDto getOrCreateStock(Long variantId, Long storeId);

    // =========================================================
    // Entrées de stock (PURCHASE, RETURN, ADJUSTMENT_IN)
    // =========================================================

    StockMovementResponseDto processEntry(StockEntryRequestDto request);

    // =========================================================
    // Sorties de stock (SALE, DAMAGE, LOSS, ADJUSTMENT_OUT)
    // =========================================================

    /**
     * Si le stock disponible est insuffisant, lance InsufficientStockException.
     */
    StockMovementResponseDto processExit(StockExitRequestDto request);

    // =========================================================
    // Réservations
    // =========================================================

    StockReservationResponseDto reserveStock(StockReservationRequestDto request);

    void releaseReservation(Long reservationId);

    void fulfilReservation(Long reservationId);

    /** Libère automatiquement les réservations expirées */
    void releaseExpiredReservations();

    // =========================================================
    // Alertes
    // =========================================================

    Page<StockAlertResponseDto> getAlerts(Pageable pageable);

    Page<StockAlertResponseDto> getAlertsByStore(Long storeId, Pageable pageable);

    void resolveAlert(Long alertId);

    /**
     * Vérifie tous les stocks et génère les alertes manquantes.
     * Peut être appelé manuellement ou déclenché après chaque mouvement.
     */
    void checkAndGenerateAlerts(Long stockId);

    // =========================================================
    // Historique des mouvements
    // =========================================================

    Page<StockMovementResponseDto> getMovements(StockMovementSearchFilter filter, Pageable pageable);

    Page<StockMovementResponseDto> getMovementsByVariant(Long variantId, Pageable pageable);

    Page<StockMovementResponseDto> getMovementsByStore(Long storeId, Pageable pageable);

    Page<StockMovementResponseDto> getMovementsByProduct(Long productId, Pageable pageable);

    // =========================================================
    // Mise à jour des seuils
    // =========================================================

    StockResponseDto updateThresholds(Long stockId, Integer minimumStock, Integer maximumStock, Integer reorderPoint);
}
