package com.optician.backend.controller;

import com.optician.backend.dto.ProductVariantRequestDto;
import com.optician.backend.dto.ProductVariantResponseDto;
import com.optician.backend.security.ProductPermissions;
import com.optician.backend.service.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Variantes Produits", description = "Gestion des variantes (couleur, taille, SKU, prix) pour chaque modèle de produit")
public class ProductVariantController {

    private final ProductVariantService variantService;

    @PostMapping("/api/products/{id}/variants")
    @PreAuthorize(ProductPermissions.HAS_CREATE_PERMISSION)
    @Operation(summary = "Ajouter une variante à un produit")
    public ResponseEntity<ProductVariantResponseDto> addVariant(
            @PathVariable("id") Long productId,
            @Valid @RequestBody ProductVariantRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variantService.addVariant(productId, dto));
    }

    @GetMapping("/api/products/{id}/variants")
    @Operation(summary = "Obtenir toutes les variantes d'un produit")
    public ResponseEntity<List<ProductVariantResponseDto>> getVariantsByProductId(@PathVariable("id") Long productId) {
        return ResponseEntity.ok(variantService.getVariantsByProductId(productId));
    }

    @PutMapping("/api/product-variants/{id}")
    @PreAuthorize(ProductPermissions.HAS_UPDATE_PERMISSION)
    @Operation(summary = "Mettre à jour une variante")
    public ResponseEntity<ProductVariantResponseDto> updateVariant(
            @PathVariable("id") Long variantId,
            @Valid @RequestBody ProductVariantRequestDto dto) {
        return ResponseEntity.ok(variantService.updateVariant(variantId, dto));
    }

    @DeleteMapping("/api/product-variants/{id}")
    @PreAuthorize(ProductPermissions.HAS_DELETE_PERMISSION)
    @Operation(summary = "Supprimer une variante")
    public ResponseEntity<Void> deleteVariant(@PathVariable("id") Long variantId) {
        variantService.deleteVariant(variantId);
        return ResponseEntity.noContent().build();
    }
}
