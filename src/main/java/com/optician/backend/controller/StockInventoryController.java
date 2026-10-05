package com.optician.backend.controller;

import com.optician.backend.dto.StockInventoryRequestDto;
import com.optician.backend.dto.StockInventoryResponseDto;
import com.optician.backend.security.StockPermissions;
import com.optician.backend.service.impl.StockInventoryServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
@Tag(name = "Inventaires", description = "Inventaire physique par magasin avec génération automatique des corrections de stock")
public class StockInventoryController {

    private final StockInventoryServiceImpl inventoryService;

    @PostMapping
    @PreAuthorize(StockPermissions.HAS_INVENTORY_PERMISSION)
    @Operation(summary = "Créer et lancer un inventaire physique")
    public ResponseEntity<StockInventoryResponseDto> create(@Valid @RequestBody StockInventoryRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createInventory(request));
    }

    @GetMapping
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Lister tous les inventaires")
    public ResponseEntity<Page<StockInventoryResponseDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        return ResponseEntity.ok(inventoryService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Détail d'un inventaire avec ses articles et écarts")
    public ResponseEntity<StockInventoryResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getById(id));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Historique des inventaires d'un magasin")
    public ResponseEntity<Page<StockInventoryResponseDto>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(inventoryService.getByStore(storeId, pageable));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize(StockPermissions.HAS_INVENTORY_PERMISSION)
    @Operation(summary = "Valider un inventaire. Génère les StockMovement INVENTORY_CORRECTION pour chaque écart.")
    public ResponseEntity<StockInventoryResponseDto> validate(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.validateInventory(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(StockPermissions.HAS_INVENTORY_PERMISSION)
    @Operation(summary = "Annuler un inventaire en cours")
    public ResponseEntity<StockInventoryResponseDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.cancelInventory(id));
    }
}
