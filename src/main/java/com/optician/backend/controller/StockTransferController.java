package com.optician.backend.controller;

import com.optician.backend.dto.StockTransferRequestDto;
import com.optician.backend.dto.StockTransferResponseDto;
import com.optician.backend.security.StockPermissions;
import com.optician.backend.service.impl.StockTransferServiceImpl;
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
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
@Tag(name = "Transferts de Stock", description = "Gestion des transferts inter-magasins avec workflow complet")
public class StockTransferController {

    private final StockTransferServiceImpl transferService;

    @PostMapping
    @PreAuthorize(StockPermissions.HAS_TRANSFER_PERMISSION)
    @Operation(summary = "Créer un transfert de stock entre magasins")
    public ResponseEntity<StockTransferResponseDto> create(@Valid @RequestBody StockTransferRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.createTransfer(request));
    }

    @GetMapping
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Lister tous les transferts")
    public ResponseEntity<Page<StockTransferResponseDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        return ResponseEntity.ok(transferService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(StockPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Détail d'un transfert")
    public ResponseEntity<StockTransferResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.getById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(StockPermissions.HAS_TRANSFER_PERMISSION)
    @Operation(summary = "Approuver un transfert (DRAFT → APPROVED)")
    public ResponseEntity<StockTransferResponseDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.approveTransfer(id));
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize(StockPermissions.HAS_TRANSFER_PERMISSION)
    @Operation(summary = "Expédier un transfert (APPROVED → IN_TRANSIT). Déclenche TRANSFER_OUT sur le stock source.")
    public ResponseEntity<StockTransferResponseDto> ship(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.shipTransfer(id));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize(StockPermissions.HAS_TRANSFER_PERMISSION)
    @Operation(summary = "Réceptionner un transfert (IN_TRANSIT → RECEIVED). Déclenche TRANSFER_IN sur le stock cible.")
    public ResponseEntity<StockTransferResponseDto> receive(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.receiveTransfer(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(StockPermissions.HAS_TRANSFER_PERMISSION)
    @Operation(summary = "Annuler un transfert. Si IN_TRANSIT, réintègre le stock source.")
    public ResponseEntity<StockTransferResponseDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.cancelTransfer(id));
    }
}
