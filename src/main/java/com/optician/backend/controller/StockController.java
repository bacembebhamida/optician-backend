package com.optician.backend.controller;

import com.optician.backend.dto.*;
import com.optician.backend.security.StockPermissions;
import com.optician.backend.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Gestion du stock par variante et par magasin")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Lister tous les stocks avec pagination")
    public ResponseEntity<Page<StockResponseDto>> getAllStocks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        return ResponseEntity.ok(stockService.getAllStocks(new StockSearchFilter(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Détail d'un enregistrement stock par ID")
    public ResponseEntity<StockResponseDto> getStockById(@PathVariable Long id) {
        return ResponseEntity.ok(stockService.getStockById(id));
    }

    @GetMapping("/variant/{variantId}")
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Stock par variante (tous les magasins)")
    public ResponseEntity<List<StockResponseDto>> getStockByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(stockService.getStockByVariantId(variantId));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Stock par produit (tous les magasins, toutes les variantes)")
    public ResponseEntity<List<StockResponseDto>> getStockByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getStockByProductId(productId));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Stock d'un magasin (toutes les variantes)")
    public ResponseEntity<List<StockResponseDto>> getStockByStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(stockService.getStockByStoreId(storeId));
    }

    @GetMapping("/variant/{variantId}/store/{storeId}")
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Obtenir ou créer le stock pour une variante dans un magasin")
    public ResponseEntity<StockResponseDto> getOrCreateStock(
            @PathVariable Long variantId,
            @PathVariable Long storeId) {
        return ResponseEntity.ok(stockService.getOrCreateStock(variantId, storeId));
    }

    // =========================================================
    // Entrées de stock
    // =========================================================

    @PostMapping("/entries")
    @PreAuthorize(StockPermissions.HAS_ENTRY_PERMISSION)
    @Operation(summary = "Entrée de stock (achat fournisseur, retour client, transfert entrant, correction positive)")
    public ResponseEntity<StockMovementResponseDto> processEntry(@Valid @RequestBody StockEntryRequestDto request) {
        return ResponseEntity.ok(stockService.processEntry(request));
    }

    // =========================================================
    // Sorties de stock
    // =========================================================

    @PostMapping("/exits")
    @PreAuthorize(StockPermissions.HAS_EXIT_PERMISSION)
    @Operation(summary = "Sortie de stock (vente, perte, casse, correction négative)")
    public ResponseEntity<StockMovementResponseDto> processExit(@Valid @RequestBody StockExitRequestDto request) {
        return ResponseEntity.ok(stockService.processExit(request));
    }

    // =========================================================
    // Réservations
    // =========================================================

    @PostMapping("/reservations")
    @PreAuthorize(StockPermissions.HAS_EXIT_PERMISSION)
    @Operation(summary = "Réserver du stock pour une commande")
    public ResponseEntity<StockReservationResponseDto> reserveStock(@Valid @RequestBody StockReservationRequestDto request) {
        return ResponseEntity.ok(stockService.reserveStock(request));
    }

    @DeleteMapping("/reservations/{id}")
    @PreAuthorize(StockPermissions.HAS_EXIT_PERMISSION)
    @Operation(summary = "Libérer une réservation (annulation commande)")
    public ResponseEntity<Void> releaseReservation(@PathVariable Long id) {
        stockService.releaseReservation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reservations/{id}/fulfil")
    @PreAuthorize(StockPermissions.HAS_EXIT_PERMISSION)
    @Operation(summary = "Satisfaire une réservation (commande livrée)")
    public ResponseEntity<Void> fulfilReservation(@PathVariable Long id) {
        stockService.fulfilReservation(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // Seuils
    // =========================================================

    @PatchMapping("/{id}/thresholds")
    @PreAuthorize(StockPermissions.HAS_ADJUST_PERMISSION)
    @Operation(summary = "Mettre à jour les seuils de stock (minimum, maximum, réapprovisionnement)")
    public ResponseEntity<StockResponseDto> updateThresholds(
            @PathVariable Long id,
            @RequestParam(required = false) Integer minimumStock,
            @RequestParam(required = false) Integer maximumStock,
            @RequestParam(required = false) Integer reorderPoint) {
        return ResponseEntity.ok(stockService.updateThresholds(id, minimumStock, maximumStock, reorderPoint));
    }
}
