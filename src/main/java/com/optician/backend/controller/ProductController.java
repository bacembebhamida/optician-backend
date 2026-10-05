package com.optician.backend.controller;

import com.optician.backend.dto.*;
import com.optician.backend.security.ProductPermissions;
import com.optician.backend.service.ProductAuditService;
import com.optician.backend.service.ProductService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Produits", description = "API complète de gestion des produits d'optique, montures, verres, lentilles et accessoires")
public class ProductController {

    private final ProductService productService;
    private final ProductAuditService auditService;

    @PostMapping
    @PreAuthorize(ProductPermissions.HAS_CREATE_PERMISSION)
    @Operation(summary = "Créer un nouveau produit d'optique")
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(dto));
    }

    @GetMapping
    @Operation(summary = "Obtenir ou filtrer les produits avec pagination")
    public ResponseEntity<?> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String faceShape,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        // If simple filters are passed without pagination params, return List for direct compatibility
        if (category != null || brand != null || faceShape != null) {
            ProductSearchFilter filter = ProductSearchFilter.builder()
                    .category(category)
                    .brand(brand)
                    .query(query)
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .build();
            Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
            List<ProductResponseDto> list = productService.searchProducts(filter, pageable).getContent();
            return ResponseEntity.ok(list);
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        ProductSearchFilter filter = ProductSearchFilter.builder()
                .query(query)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();

        Page<ProductResponseDto> result = productService.searchProducts(filter, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    @Operation(summary = "Recherche avancée multi-critères des produits")
    public ResponseEntity<Page<ProductResponseDto>> searchProducts(
            @ModelAttribute ProductSearchFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(productService.searchProducts(filter, pageable));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Obtenir les produits par nom de catégorie (endpoint de compatibilité frontend)")
    public ResponseEntity<List<ProductResponseDto>> getProductsByCategoryPath(@PathVariable String category) {
        ProductSearchFilter filter = ProductSearchFilter.builder().category(category).build();
        Pageable pageable = PageRequest.of(0, 100);
        return ResponseEntity.ok(productService.searchProducts(filter, pageable).getContent());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir le détail d'un produit par ID")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ProductPermissions.HAS_UPDATE_PERMISSION)
    @Operation(summary = "Mettre à jour l'intégralité d'un produit")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ProductPermissions.HAS_UPDATE_PERMISSION)
    @Operation(summary = "Mettre à jour partiellement un produit")
    public ResponseEntity<ProductResponseDto> patchProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(productService.patchProduct(id, updates));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ProductPermissions.HAS_DELETE_PERMISSION)
    @Operation(summary = "Supprimer un produit (Soft Delete)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize(ProductPermissions.HAS_UPDATE_PERMISSION)
    @Operation(summary = "Activer ou désactiver un produit")
    public ResponseEntity<ProductResponseDto> toggleProductActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(productService.toggleProductActive(id, active));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize(ProductPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Obtenir l'historique d'audit d'un produit")
    public ResponseEntity<List<ProductAuditLogDto>> getProductAuditLogs(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getAuditLogsForEntity("Product", id));
    }
}
