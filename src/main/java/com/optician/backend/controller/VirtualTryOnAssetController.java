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
@RequestMapping("/api")
@RequiredArgsConstructor
public class VirtualTryOnAssetController {

    private final VirtualTryOnAssetService tryOnAssetService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    private Long resolveVariantId(String variantIdOrSku) {
        try {
            Long numericId = Long.parseLong(variantIdOrSku);
            
            // 1. Check if numericId is a Product ID first (e.g. /admin/products/23 or 24)
            Product product = productRepository.findById(numericId).orElse(null);
            if (product != null) {
                List<ProductVariant> productVariants = variantRepository.findByProductId(product.getId());
                if (!productVariants.isEmpty()) {
                    return productVariants.get(0).getId();
                }
                // Product exists but has no variant -> auto-create a default variant for THIS product
                ProductVariant newVariant = ProductVariant.builder()
                        .product(product)
                        .sku(product.getReference() != null ? product.getReference() : "SKU-P" + product.getId())
                        .color("Standard")
                        .size("Standard")
                        .sellingPrice(java.math.BigDecimal.ZERO)
                        .active(true)
                        .build();
                return variantRepository.save(newVariant).getId();
            }

            // 2. Check if numericId is a Variant ID directly
            if (variantRepository.existsById(numericId)) {
                return numericId;
            }

            return numericId;
        } catch (NumberFormatException e) {
            ProductVariant variant = variantRepository.findBySku(variantIdOrSku)
                    .or(() -> variantRepository.findByBarcode(variantIdOrSku))
                    .orElse(null);
            if (variant != null) {
                return variant.getId();
            }
            List<ProductVariant> all = variantRepository.findAll();
            if (!all.isEmpty()) {
                return all.get(0).getId();
            }
            throw new ResourceNotFoundException("Variante introuvable avec l'id ou SKU: " + variantIdOrSku);
        }
    }

    /**
     * Obtenir tous les modèles 3D pour une variante (publiés et en revue).
     */
    @GetMapping(value = {
        "/tryon/variants/{variantId}/all",
        "/variants/{variantId}/try-on/all",
        "/variants/{variantId}/try-on",
        "/tryon/products/{variantId}/all",
        "/products/{variantId}/try-on/all",
        "/products/{variantId}/try-on"
    })
    public ResponseEntity<List<VirtualTryOnAssetResponseDto>> getAllAssetsForVariant(@PathVariable String variantId) {
        try {
            Long id = resolveVariantId(variantId);
            List<VirtualTryOnAssetResponseDto> list = tryOnAssetService.getAssetsByVariantId(id);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.warn("Aucun modèle 3D trouvé pour la variante {}: {}", variantId, e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Admin 3D Studio : Données complètes de contrôle qualité 3D pour la variante.
     */
    @GetMapping(value = {
        "/tryon/variants/{variantId}/studio", 
        "/admin/variants/{variantId}/3d-data",
        "/tryon/products/{variantId}/studio", 
        "/admin/products/{variantId}/3d-data"
    })
    public ResponseEntity<Map<String, Object>> getAdmin3dStudioData(@PathVariable String variantId) {
        Long id = resolveVariantId(variantId);
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variante introuvable: " + id));

        Product product = variant.getProduct();
        List<VirtualTryOnAssetResponseDto> assets = tryOnAssetService.getAssetsByVariantId(id);

        VirtualTryOnAssetResponseDto currentAsset = null;
        if (!assets.isEmpty()) {
            currentAsset = assets.stream().filter(a -> a.getStatus() == TryOnAssetStatus.PUBLISHED).findFirst()
                    .orElse(assets.get(0));
        }

        Map<String, Object> studioData = new HashMap<>();
        studioData.put("variantId", variant.getId());
        studioData.put("variantSku", variant.getSku());
        studioData.put("color", variant.getColor());
        studioData.put("productName", product.getName());
        studioData.put("brand", product.getBrand());
        studioData.put("frameShape", product.getFrameShape() != null ? product.getFrameShape().name() : "CARRE");
        studioData.put("frameMaterial", product.getMaterial());
        studioData.put("opticalDimensions", Map.of(
            "lensWidth", currentAsset != null && currentAsset.getOpticalLensWidth() != null ? currentAsset.getOpticalLensWidth() : 52,
            "bridgeWidth", currentAsset != null && currentAsset.getOpticalBridgeWidth() != null ? currentAsset.getOpticalBridgeWidth() : 18,
            "templeLength", currentAsset != null && currentAsset.getOpticalTempleLength() != null ? currentAsset.getOpticalTempleLength() : 140,
            "totalWidth", currentAsset != null && currentAsset.getOpticalTotalWidth() != null ? currentAsset.getOpticalTotalWidth() : 138,
            "lensHeight", currentAsset != null && currentAsset.getOpticalLensHeight() != null ? currentAsset.getOpticalLensHeight() : 42
        ));
        studioData.put("currentAsset", currentAsset);
        studioData.put("assetsHistory", assets);

        return ResponseEntity.ok(studioData);
    }

    /**
     * Obtenir le modèle 3D publié pour l'essayage virtuel à partir de l'ID variante.
     */
    @GetMapping(value = {
        "/tryon/variants/{variantId}",
        "/variants/{variantId}/try-on/published",
        "/products/{variantId}/try-on/published"
    })
    public ResponseEntity<VirtualTryOnAssetResponseDto> getAssetForVariant(@PathVariable String variantId) {
        Long id = resolveVariantId(variantId);
        VirtualTryOnAssetResponseDto dto = tryOnAssetService.getPublishedAssetByVariantId(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Obtenir le modèle 3D pour un produit (essayage virtuel gratuit client).
     */
    @GetMapping("/tryon/products/{productId}")
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
    @GetMapping("/tryon/scan/{code}")
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
    @PostMapping(value = {
        "/tryon/variants/{variantId}/upload", 
        "/variants/{variantId}/try-on/upload",
        "/tryon/products/{variantId}/upload",
        "/products/{variantId}/try-on/upload"
    }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VirtualTryOnAssetResponseDto> uploadGlbModel(
            @PathVariable String variantId,
            @RequestParam("file") MultipartFile file) {
        Long id = resolveVariantId(variantId);
        VirtualTryOnAssetResponseDto dto = tryOnAssetService.uploadGlbModel(id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Génération 3D automatique par IA à partir de photos du produit (ou fallback).
     * Supporte les deux formats de routes: /tryon/variants/{variantId}/generate ET /variants/{variantId}/try-on/generate
     */
    @PostMapping(value = {
        "/tryon/variants/{variantId}/generate",
        "/variants/{variantId}/try-on/generate",
        "/tryon/products/{variantId}/generate",
        "/products/{variantId}/try-on/generate"
    }, consumes = MediaType.ALL_VALUE)
    public ResponseEntity<VirtualTryOnAssetResponseDto> generate3dFromImages(
            @PathVariable String variantId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl) {
        Long id = resolveVariantId(variantId);
        List<MultipartFile> files = (file != null && !file.isEmpty()) ? List.of(file) : List.of();
        VirtualTryOnAssetResponseDto dto = tryOnAssetService.generateFromImages(id, files);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    /**
     * Ajustement de la calibration 3D (scale, offsets yeux/nez/branches).
     */
    @PutMapping(value = {"/tryon/assets/{assetId}/calibration", "/variants/try-on/assets/{assetId}/calibration"})
    public ResponseEntity<VirtualTryOnAssetResponseDto> updateCalibration(
            @PathVariable Long assetId,
            @RequestBody VirtualTryOnAssetRequestDto dto) {
        VirtualTryOnAssetResponseDto result = tryOnAssetService.updateCalibration(assetId, dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Publier le modèle 3D pour l'essayage virtuel client.
     */
    @PostMapping(value = {"/tryon/assets/{assetId}/publish", "/variants/try-on/assets/{assetId}/publish"})
    public ResponseEntity<VirtualTryOnAssetResponseDto> publishAsset(@PathVariable Long assetId) {
        VirtualTryOnAssetResponseDto result = tryOnAssetService.publishAsset(assetId);
        return ResponseEntity.ok(result);
    }

    /**
     * Supprimer un modèle 3D.
     */
    @DeleteMapping(value = {"/tryon/assets/{assetId}", "/variants/try-on/assets/{assetId}"})
    public ResponseEntity<Void> deleteAsset(@PathVariable Long assetId) {
        tryOnAssetService.deleteAsset(assetId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Polling d'avancement d'un job de génération 3D par son jobId.
     */
    @GetMapping("/tryon/jobs/{jobId}")
    public ResponseEntity<VirtualTryOnAssetResponseDto> getJobStatus(@PathVariable String jobId) {
        VirtualTryOnAssetResponseDto dto = tryOnAssetService.getAssetByJobId(jobId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Rejeter un modèle 3D lors du contrôle qualité admin.
     */
    @PostMapping(value = {"/tryon/assets/{assetId}/reject", "/variants/try-on/assets/{assetId}/reject"})
    public ResponseEntity<VirtualTryOnAssetResponseDto> rejectAsset(
            @PathVariable Long assetId,
            @RequestParam(value = "reason", required = false) String reason) {
        VirtualTryOnAssetResponseDto result = tryOnAssetService.rejectAsset(assetId, reason);
        return ResponseEntity.ok(result);
    }
}

