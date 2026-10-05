package com.optician.backend.controller;

import com.optician.backend.dto.VirtualTryOnAssetRequestDto;
import com.optician.backend.dto.VirtualTryOnAssetResponseDto;
import com.optician.backend.security.TryOnPermissions;
import com.optician.backend.service.VirtualTryOnAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Virtual Try-On 3D", description = "Gestion des actifs 3D GLB/glTF et calibration d'essayage virtuel")
public class VirtualTryOnAssetController {

    private final VirtualTryOnAssetService assetService;

    @GetMapping("/variants/{variantId}/try-on")
    @PreAuthorize(TryOnPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Récupérer l'actif 3D publié d'une variante")
    public ResponseEntity<VirtualTryOnAssetResponseDto> getPublishedAssetByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(assetService.getPublishedAssetByVariantId(variantId));
    }

    @GetMapping("/variants/{variantId}/try-on/all")
    @PreAuthorize(TryOnPermissions.HAS_VIEW_PERMISSION)
    @Operation(summary = "Lister tous les actifs 3D d'une variante")
    public ResponseEntity<List<VirtualTryOnAssetResponseDto>> getAllAssetsByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(assetService.getAssetsByVariantId(variantId));
    }

    @PostMapping(value = "/variants/{variantId}/try-on/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(TryOnPermissions.HAS_CREATE_PERMISSION)
    @Operation(summary = "Importer un fichier 3D (GLB / glTF) pour une variante")
    public ResponseEntity<VirtualTryOnAssetResponseDto> uploadGlbModel(
            @PathVariable Long variantId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.uploadGlbModel(variantId, file));
    }

    @PostMapping(value = "/variants/{variantId}/try-on/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(TryOnPermissions.HAS_CREATE_PERMISSION)
    @Operation(summary = "Générer un modèle 3D via IA locale depuis des photos (Face, 3/4, Côté)")
    public ResponseEntity<VirtualTryOnAssetResponseDto> generateFromImages(
            @PathVariable Long variantId,
            @RequestParam("images") List<MultipartFile> images) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(assetService.generateFromImages(variantId, images));
    }

    @PutMapping("/try-on-assets/{id}")
    @PreAuthorize(TryOnPermissions.HAS_UPDATE_PERMISSION)
    @Operation(summary = "Mettre à jour la calibration (Échelle, offsets X/Y/Z, rotations)")
    public ResponseEntity<VirtualTryOnAssetResponseDto> updateCalibration(
            @PathVariable Long id,
            @RequestBody VirtualTryOnAssetRequestDto dto) {
        return ResponseEntity.ok(assetService.updateCalibration(id, dto));
    }

    @PostMapping("/try-on-assets/{id}/validate")
    @PreAuthorize(TryOnPermissions.HAS_VALIDATE_PERMISSION)
    @Operation(summary = "Valider un modèle 3D généré ou importé")
    public ResponseEntity<VirtualTryOnAssetResponseDto> validateAsset(
            @PathVariable Long id,
            Authentication authentication) {
        String validator = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.ok(assetService.validateAsset(id, validator));
    }

    @PostMapping("/try-on-assets/{id}/publish")
    @PreAuthorize(TryOnPermissions.HAS_PUBLISH_PERMISSION)
    @Operation(summary = "Publier un modèle 3D pour l'essayage client")
    public ResponseEntity<VirtualTryOnAssetResponseDto> publishAsset(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.publishAsset(id));
    }

    @DeleteMapping("/try-on-assets/{id}")
    @PreAuthorize(TryOnPermissions.HAS_DELETE_PERMISSION)
    @Operation(summary = "Supprimer un actif 3D")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }
}
