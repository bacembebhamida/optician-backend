package com.optician.backend.controller;

import com.optician.backend.dto.StockMovementResponseDto;
import com.optician.backend.dto.StockMovementSearchFilter;
import com.optician.backend.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
@Tag(name = "Mouvements de Stock", description = "Historique immutable de tous les mouvements de stock")
public class StockMovementController {

    private final StockService stockService;

    @GetMapping
    @Operation(summary = "Lister tous les mouvements de stock")
    public ResponseEntity<Page<StockMovementResponseDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        return ResponseEntity.ok(stockService.getMovements(new StockMovementSearchFilter(), pageable));
    }

    @GetMapping("/variant/{variantId}")
    @Operation(summary = "Mouvements par variante de produit")
    public ResponseEntity<Page<StockMovementResponseDto>> getByVariant(
            @PathVariable Long variantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(stockService.getMovementsByVariant(variantId, pageable));
    }

    @GetMapping("/store/{storeId}")
    @Operation(summary = "Mouvements d'un magasin")
    public ResponseEntity<Page<StockMovementResponseDto>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(stockService.getMovementsByStore(storeId, pageable));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Mouvements d'un produit (toutes variantes)")
    public ResponseEntity<Page<StockMovementResponseDto>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(stockService.getMovementsByProduct(productId, pageable));
    }
}
