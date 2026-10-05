package com.optician.backend.controller;

import com.optician.backend.dto.VirtualTryOnAssetRequestDto;
import com.optician.backend.dto.VirtualTryOnAssetResponseDto;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.model.Product;
import com.optician.backend.model.ProductVariant;
import com.optician.backend.model.enums.TryOnAssetStatus;
import com.optician.backend.repository.ProductRepository;
import com.optician.backend.repository.ProductVariantRepository;
import com.optician.backend.service.VirtualTryOnAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tryon")
@RequiredArgsConstructor
public class VirtualTryOnAssetController {

    private final VirtualTryOnAssetService tryOnAssetService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    /**
     * Obtenir le modèle 3D publié pour l'essayage virtuel à partir de l'ID variante.
     */
    @GetMapping("/variants/{variantId}")
    public ResponseEntity<VirtualTryOnAssetResponseDto> getAssetForVariant(@PathVariable Long variantId) {
        VirtualTryOnAssetResponseDto dto = tryOnAssetService.getPublishedAssetByVariantId(variantId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Obtenir le modèle 3D pour un produit (essayage virtuel gratuit client).
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<VirtualTryOnAssetResponseDto> getAssetForProduct(@PathVariable Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable avec l'id: " + productId));

        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        if (!variants.isEmpty()) {
            try {
                VirtualTryOnAssetResponseDto dto = tryOnAssetService.getPublishedAssetByVariantId(variants.get(0).getId());
                return ResponseEntity.ok(dto);
            } catch (Exception e) {
                log.warn("Aucun modèle 3D publié pour les variantes de {}, génération fallback", product.getName());
            }
        }

        // Fallback 3D config pour essayage virtuel
        String shape = product.getFrameShape() != null ? product.getFrameShape().name().toLowerCase() : "rectangle";
        String modelUrl = product.getModel3dUrl() != null && !product.getModel3dUrl().isBlank()
                ? product.getModel3dUrl()
                : "/uploads/models/procedural_" + shape + ".glb";

        VirtualTryOnAssetResponseDto fallback = VirtualTryOnAssetResponseDto.builder()
                .id(0L)
                .variantId(variants.isEmpty() ? null : variants.get(0).getId())
                .modelUrl(modelUrl)
                .thumbnailUrl(product.getImageUrl())
                .format("GLB")
                .status(TryOnAssetStatus.PUBLISHED)
                .version(1)
                .scale(1.0)
                .positionX(0.0)
                .positionY(0.0)
                .positionZ(0.0)
                .rotationX(0.0)
                .rotationY(0.0)
                .rotationZ(0.0)
                .build();

        return ResponseEntity.ok(fallback);
    }

    /**
     * SCAN CAMERA / CODE-BARRES : Recherche d'un produit par code-barres ou SKU
     * et retour immédiat du modèle 3D pour l'essayage virtuel et gestion stock.
     */
    @GetMapping("/scan/{code}")
    public ResponseEntity<Map<String, Object>> scanBarcodeOrSku(@PathVariable String code) {
        String cleanCode = code.trim();
        ProductVariant variant = variantRepository.findByBarcode(cleanCode)
                .or(() -> variantRepository.findBySku(cleanCode))
                .orElse(null);

        Map<String, Object> response = new HashMap<>();

        if (variant != null) {
            Product product = variant.getProduct();
            VirtualTryOnAssetResponseDto assetDto = null;
            try {
                assetDto = tryOnAssetService.getPublishedAssetByVariantId(variant.getId());
            } catch (Exception e) {
                String shape = product.getFrameShape() != null ? product.getFrameShape().name().toLowerCase() : "rectangle";
                assetDto = VirtualTryOnAssetResponseDto.builder()
                        .variantId(variant.getId())
                        .modelUrl("/uploads/models/procedural_" + shape + ".glb")
                        .format("GLB")
                        .status(TryOnAssetStatus.PUBLISHED)
                        .scale(1.0)
                        .build();
            }

            response.put("found", true);
            response.put("scanType", "VARIANT");
            response.put("productId", product.getId());
            response.put("productName", product.getName());
            response.put("reference", product.getReference());
            response.put("variantId", variant.getId());
            response.put("sku", variant.getSku());
            response.put("barcode", variant.getBarcode());
            response.put("color", variant.getColor());
            response.put("price", variant.getSellingPrice());
            response.put("tryOn3dAvailable", true);
            response.put("tryOnAsset", assetDto);
            return ResponseEntity.ok(response);
        }

        // Search by Product reference
        Product product = productRepository.findByReference(cleanCode).orElse(null);
        if (product != null) {
            String shape = product.getFrameShape() != null ? product.getFrameShape().name().toLowerCase() : "rectangle";
            VirtualTryOnAssetResponseDto assetDto = VirtualTryOnAssetResponseDto.builder()
                    .modelUrl("/uploads/models/procedural_" + shape + ".glb")
                    .format("GLB")
                    .status(TryOnAssetStatus.PUBLISHED)
                    .scale(1.0)
                    .build();

            response.put("found", true);
            response.put("scanType", "PRODUCT");
            response.put("productId", product.getId());
            response.put("productName", product.getName());
            response.put("reference", product.getReference());
            response.put("tryOn3dAvailable", true);
            response.put("tryOnAsset", assetDto);
            return ResponseEntity.ok(response);
        }

        response.put("found", false);
        response.put("message", "Aucun produit ou variante correspondant au code scanné: " + cleanCode);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Upload d'un fichier 3D GLB pour une variante (Admin/Opticien).
     */
    @PostMapping(value = "/variants/{variantId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('TRYON_MANAGE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VirtualTryOnAssetResponseDto> uploadGlbModel(
            @PathVariable Long variantId,
            @RequestParam("file") MultipartFile file) {
        VirtualTryOnAssetResponseDto dto = tryOnAssetService.uploadGlbModel(variantId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Génération 3D automatique par IA à partir de photos du produit.
     */
    @PostMapping(value = "/variants/{variantId}/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('TRYON_MANAGE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VirtualTryOnAssetResponseDto> generate3dFromImages(
            @PathVariable Long variantId,
            @RequestParam("images") List<MultipartFile> images) {
        VirtualTryOnAssetResponseDto dto = tryOnAssetService.generateFromImages(variantId, images);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    /**
     * Ajustement de la calibration 3D (scale, offsets yeux/nez/branches).
     */
    @PutMapping("/assets/{assetId}/calibration")
    @PreAuthorize("hasAuthority('TRYON_MANAGE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VirtualTryOnAssetResponseDto> updateCalibration(
            @PathVariable Long assetId,
            @RequestBody VirtualTryOnAssetRequestDto dto) {
        VirtualTryOnAssetResponseDto result = tryOnAssetService.updateCalibration(assetId, dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Publier le modèle 3D pour l'essayage virtuel client.
     */
    @PostMapping("/assets/{assetId}/publish")
    @PreAuthorize("hasAuthority('TRYON_MANAGE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<VirtualTryOnAssetResponseDto> publishAsset(@PathVariable Long assetId) {
        VirtualTryOnAssetResponseDto result = tryOnAssetService.publishAsset(assetId);
        return ResponseEntity.ok(result);
    }

    /**
     * Supprimer un modèle 3D.
     */
    @DeleteMapping("/assets/{assetId}")
    @PreAuthorize("hasAuthority('TRYON_MANAGE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long assetId) {
        tryOnAssetService.deleteAsset(assetId);
        return ResponseEntity.noContent().build();
    }
}
