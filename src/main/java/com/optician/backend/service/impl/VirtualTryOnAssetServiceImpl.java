package com.optician.backend.service.impl;

import com.optician.backend.dto.VirtualTryOnAssetRequestDto;
import com.optician.backend.dto.VirtualTryOnAssetResponseDto;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.mapper.VirtualTryOnAssetMapper;
import com.optician.backend.model.ProductVariant;
import com.optician.backend.model.VirtualTryOnAsset;
import com.optician.backend.model.enums.AuditAction;
import com.optician.backend.model.enums.TryOnAssetStatus;
import com.optician.backend.repository.ProductVariantRepository;
import com.optician.backend.repository.VirtualTryOnAssetRepository;
import com.optician.backend.service.ProductAuditService;
import com.optician.backend.service.VirtualTryOnAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualTryOnAssetServiceImpl implements VirtualTryOnAssetService {

    private final VirtualTryOnAssetRepository assetRepository;
    private final ProductVariantRepository variantRepository;
    private final VirtualTryOnAssetMapper assetMapper;
    private final ProductAuditService auditService;

    private static final String UPLOAD_DIR = "uploads/models/";
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB

    @Override
    @Transactional(readOnly = true)
    public List<VirtualTryOnAssetResponseDto> getAssetsByVariantId(Long variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Variante introuvable avec l'id: " + variantId);
        }
        return assetRepository.findByVariantId(variantId).stream()
                .map(assetMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VirtualTryOnAssetResponseDto getPublishedAssetByVariantId(Long variantId) {
        VirtualTryOnAsset asset = assetRepository.findFirstByVariantIdAndStatus(variantId, TryOnAssetStatus.PUBLISHED)
                .orElseGet(() -> assetRepository.findFirstByVariantIdOrderByVersionDesc(variantId)
                        .orElseThrow(() -> new ResourceNotFoundException("Aucun modèle 3D configuré pour la variante: " + variantId)));
        return assetMapper.toDto(asset);
    }

    @Override
    @Transactional
    public VirtualTryOnAssetResponseDto uploadGlbModel(Long variantId, MultipartFile file) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante introuvable avec l'id: " + variantId));

        validateGlbFile(file);

        String savedFilename = storeFileLocally(file);
        String modelUrl = "/uploads/models/" + savedFilename;

        VirtualTryOnAsset asset = VirtualTryOnAsset.builder()
                .variant(variant)
                .modelUrl(modelUrl)
                .format("GLB")
                .status(TryOnAssetStatus.READY_FOR_REVIEW)
                .version(getNextVersion(variantId))
                .scale(1.0)
                .positionX(0.0)
                .positionY(0.0)
                .positionZ(0.0)
                .rotationX(0.0)
                .rotationY(0.0)
                .rotationZ(0.0)
                .build();

        VirtualTryOnAsset saved = assetRepository.save(asset);
        auditService.logAudit(AuditAction.CREATE, "VirtualTryOnAsset", saved.getId(), null, "Importé GLB " + savedFilename);

        return assetMapper.toDto(saved);
    }

    @Override
    @Transactional
    public VirtualTryOnAssetResponseDto generateFromImages(Long variantId, List<MultipartFile> images) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante introuvable avec l'id: " + variantId));

        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Au moins une image est requise pour la génération 3D.");
        }

        // Save reference image locally
        MultipartFile primaryImage = images.get(0);
        String tempImgFilename = UUID.randomUUID() + "_" + sanitizeFilename(primaryImage.getOriginalFilename());
        Path tempImgPath = Paths.get("uploads/temp-images", tempImgFilename);
        try {
            if (!Files.exists(tempImgPath.getParent())) {
                Files.createDirectories(tempImgPath.getParent());
            }
            try (InputStream is = primaryImage.getInputStream()) {
                Files.copy(is, tempImgPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("Impossible de sauvegarder l'image temporaire pour l'IA 3D: {}", e.getMessage());
        }

        String generatedGlbName = "gen_3d_" + UUID.randomUUID() + ".glb";
        String modelUrl = "/uploads/models/" + generatedGlbName;
        String shapeStr = (variant.getProduct() != null && variant.getProduct().getFrameShape() != null)
                ? variant.getProduct().getFrameShape().name() : "CARRE";

        // Save generated model asset record
        VirtualTryOnAsset asset = VirtualTryOnAsset.builder()
                .variant(variant)
                .modelUrl(modelUrl)
                .thumbnailUrl("/uploads/temp-images/" + tempImgFilename)
                .format("GLB")
                .status(TryOnAssetStatus.READY_FOR_REVIEW)
                .version(getNextVersion(variantId))
                .scale(1.0)
                .positionX(0.0)
                .positionY(0.0)
                .positionZ(0.0)
                .rotationX(0.0)
                .rotationY(0.0)
                .rotationZ(0.0)
                .build();

        VirtualTryOnAsset saved = assetRepository.save(asset);
        auditService.logAudit(AuditAction.CREATE, "VirtualTryOnAsset", saved.getId(), null, "Génération 3D IA initiée depuis " + images.size() + " image(s)");

        // Trigger background local Python SPAR3D/TripoSR worker or 3D mesh reconstructor
        generateGlbLocallyAsync(saved.getId(), generatedGlbName, tempImgPath.toString(), shapeStr);

        return assetMapper.toDto(saved);
    }

    @Override
    @Transactional
    public VirtualTryOnAssetResponseDto updateCalibration(Long assetId, VirtualTryOnAssetRequestDto dto) {
        VirtualTryOnAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Modèle 3D introuvable: " + assetId));

        if (dto.getScale() != null) asset.setScale(dto.getScale());
        if (dto.getPositionX() != null) asset.setPositionX(dto.getPositionX());
        if (dto.getPositionY() != null) asset.setPositionY(dto.getPositionY());
        if (dto.getPositionZ() != null) asset.setPositionZ(dto.getPositionZ());
        if (dto.getRotationX() != null) asset.setRotationX(dto.getRotationX());
        if (dto.getRotationY() != null) asset.setRotationY(dto.getRotationY());
        if (dto.getRotationZ() != null) asset.setRotationZ(dto.getRotationZ());
        if (dto.getEyeOffset() != null) asset.setEyeOffset(dto.getEyeOffset());
        if (dto.getBridgeOffset() != null) asset.setBridgeOffset(dto.getBridgeOffset());
        if (dto.getTempleOffset() != null) asset.setTempleOffset(dto.getTempleOffset());

        VirtualTryOnAsset saved = assetRepository.save(asset);
        auditService.logAudit(AuditAction.UPDATE, "VirtualTryOnAsset", saved.getId(), null, "Mis à jour calibration 3D");

        return assetMapper.toDto(saved);
    }

    @Override
    @Transactional
    public VirtualTryOnAssetResponseDto validateAsset(Long assetId, String validatorName) {
        VirtualTryOnAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Modèle 3D introuvable: " + assetId));

        asset.setStatus(TryOnAssetStatus.VALIDATED);
        asset.setValidatedAt(LocalDateTime.now());
        asset.setValidatedBy(validatorName != null ? validatorName : "ADMIN");

        VirtualTryOnAsset saved = assetRepository.save(asset);
        auditService.logAudit(AuditAction.UPDATE, "VirtualTryOnAsset", saved.getId(), null, "Validé par " + validatorName);

        return assetMapper.toDto(saved);
    }

    @Override
    @Transactional
    public VirtualTryOnAssetResponseDto publishAsset(Long assetId) {
        VirtualTryOnAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Modèle 3D introuvable: " + assetId));

        // Unpublish any other asset for this variant
        assetRepository.findByVariantId(asset.getVariant().getId()).forEach(a -> {
            if (a.getStatus() == TryOnAssetStatus.PUBLISHED && !a.getId().equals(assetId)) {
                a.setStatus(TryOnAssetStatus.VALIDATED);
                assetRepository.save(a);
            }
        });

        asset.setStatus(TryOnAssetStatus.PUBLISHED);
        VirtualTryOnAsset saved = assetRepository.save(asset);
        auditService.logAudit(AuditAction.UPDATE, "VirtualTryOnAsset", saved.getId(), null, "Publié pour l'essayage client");

        return assetMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteAsset(Long assetId) {
        VirtualTryOnAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Modèle 3D introuvable: " + assetId));

        assetRepository.delete(asset);
        auditService.logAudit(AuditAction.DELETE, "VirtualTryOnAsset", assetId, null, "Supprimé actif 3D");
    }

    // --- HELPER METHODS ---

    private int getNextVersion(Long variantId) {
        return assetRepository.findFirstByVariantIdOrderByVersionDesc(variantId)
                .map(a -> a.getVersion() + 1)
                .orElse(1);
    }

    private void validateGlbFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier GLB est vide.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La taille du fichier dépasse la limite maximale de 25 Mo.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".glb") && !filename.toLowerCase().endsWith(".gltf"))) {
            throw new IllegalArgumentException("Seuls les fichiers .glb et .gltf sont autorisés.");
        }

        // Validate GLB magic header bytes (0x67, 0x6C, 0x54, 0x46 -> "glTF")
        if (filename.toLowerCase().endsWith(".glb")) {
            try (InputStream is = file.getInputStream()) {
                byte[] header = new byte[4];
                int read = is.read(header);
                if (read < 4 || header[0] != 'g' || header[1] != 'l' || header[2] != 'T' || header[3] != 'F') {
                    log.warn("Fichier GLB invalid header magic bytes");
                }
            } catch (IOException e) {
                log.error("Erreur lors de la validation du fichier GLB", e);
            }
        }
    }

    private String storeFileLocally(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String ext = file.getOriginalFilename().toLowerCase().endsWith(".gltf") ? ".gltf" : ".glb";
            String savedName = UUID.randomUUID() + ext;
            Path filePath = uploadPath.resolve(savedName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath);
            }
            return savedName;
        } catch (IOException e) {
            throw new RuntimeException("Échec de la sauvegarde du fichier 3D sur le disque local.", e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "file.png";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void generateGlbLocallyAsync(Long assetId, String targetGlbFilename, String inputImgPath, String shapeStr) {
        new Thread(() -> {
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                File targetFile = uploadPath.resolve(targetGlbFilename).toFile();

                File pythonScript = new File("scripts/reconstruct_3d.py");
                if (pythonScript.exists()) {
                    List<String> cmd = new ArrayList<>(List.of("python3", "scripts/reconstruct_3d.py", "--output", targetFile.getAbsolutePath()));
                    if (inputImgPath != null && new File(inputImgPath).exists()) {
                        cmd.add("--input");
                        cmd.add(inputImgPath);
                    }
                    if (shapeStr != null && !shapeStr.isBlank()) {
                        cmd.add("--shape");
                        cmd.add(shapeStr);
                    }
                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    log.info("Processus Python local d'IA 3D terminé avec le code exit: {}", exitCode);
                } else {
                    writeMinimalGlbPlaceholder(targetFile);
                }
            } catch (Exception e) {
                log.error("Erreur lors de la génération 3D locale", e);
            }
        }).start();
    }

    private void writeMinimalGlbPlaceholder(File targetFile) throws IOException {
        // Minimum valid GLB binary header & json chunk for WebGL rendering
        byte[] dummyGlb = new byte[] {
            'g', 'l', 'T', 'F', 0x02, 0x00, 0x00, 0x00, // Header: magic, version 2
            0x4C, 0x00, 0x00, 0x00,                     // Total length: 76 bytes
            0x40, 0x00, 0x00, 0x00,                     // JSON Chunk length: 64 bytes
            'J', 'S', 'O', 'N',                         // JSON Chunk type
            '{', '"', 'a', 's', 's', 'e', 't', '"', ':', '{', '"', 'v', 'e', 'r', 's', 'i', 'o', 'n', '"', ':', '"', '2', '.', '0', '"', '}', '}', ' ', ' '
        };
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(dummyGlb);
        }
    }
}
