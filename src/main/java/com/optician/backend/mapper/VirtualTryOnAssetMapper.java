package com.optician.backend.mapper;

import com.optician.backend.dto.VirtualTryOnAssetRequestDto;
import com.optician.backend.dto.VirtualTryOnAssetResponseDto;
import com.optician.backend.model.VirtualTryOnAsset;
import org.springframework.stereotype.Component;

@Component
public class VirtualTryOnAssetMapper {

    public VirtualTryOnAsset toEntity(VirtualTryOnAssetRequestDto dto) {
        if (dto == null) return null;
        return VirtualTryOnAsset.builder()
                .modelUrl(dto.getModelUrl())
                .thumbnailUrl(dto.getThumbnailUrl())
                .format(dto.getFormat() != null ? dto.getFormat() : "GLB")
                .status(dto.getStatus() != null ? dto.getStatus() : com.optician.backend.model.enums.TryOnAssetStatus.DRAFT)
                .version(dto.getVersion() != null ? dto.getVersion() : 1)
                .scale(dto.getScale() != null ? dto.getScale() : 1.0)
                .positionX(dto.getPositionX() != null ? dto.getPositionX() : 0.0)
                .positionY(dto.getPositionY() != null ? dto.getPositionY() : 0.0)
                .positionZ(dto.getPositionZ() != null ? dto.getPositionZ() : 0.0)
                .rotationX(dto.getRotationX() != null ? dto.getRotationX() : 0.0)
                .rotationY(dto.getRotationY() != null ? dto.getRotationY() : 0.0)
                .rotationZ(dto.getRotationZ() != null ? dto.getRotationZ() : 0.0)
                .eyeOffset(dto.getEyeOffset() != null ? dto.getEyeOffset() : 0.0)
                .bridgeOffset(dto.getBridgeOffset() != null ? dto.getBridgeOffset() : 0.0)
                .templeOffset(dto.getTempleOffset() != null ? dto.getTempleOffset() : 0.0)
                .build();
    }

    public VirtualTryOnAssetResponseDto toDto(VirtualTryOnAsset entity) {
        if (entity == null) return null;
        return VirtualTryOnAssetResponseDto.builder()
                .id(entity.getId())
                .variantId(entity.getVariant() != null ? entity.getVariant().getId() : null)
                .variantSku(entity.getVariant() != null ? entity.getVariant().getSku() : null)
                .modelUrl(entity.getModelUrl())
                .thumbnailUrl(entity.getThumbnailUrl())
                .format(entity.getFormat())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .jobId(entity.getJobId())
                .qualityScore(entity.getQualityScore())
                .geometryScore(entity.getGeometryScore())
                .symmetryScore(entity.getSymmetryScore())
                .scaleScore(entity.getScaleScore())
                .materialScore(entity.getMaterialScore())
                .visualSimilarityScore(entity.getVisualSimilarityScore())
                .renderFrontUrl(entity.getRenderFrontUrl())
                .renderThreeQuarterUrl(entity.getRenderThreeQuarterUrl())
                .renderSideUrl(entity.getRenderSideUrl())
                .renderBackUrl(entity.getRenderBackUrl())
                .statusDetails(entity.getStatusDetails())
                .opticalLensWidth(entity.getOpticalLensWidth())
                .opticalBridgeWidth(entity.getOpticalBridgeWidth())
                .opticalTempleLength(entity.getOpticalTempleLength())
                .opticalTotalWidth(entity.getOpticalTotalWidth())
                .opticalLensHeight(entity.getOpticalLensHeight())
                .scale(entity.getScale())
                .positionX(entity.getPositionX())
                .positionY(entity.getPositionY())
                .positionZ(entity.getPositionZ())
                .rotationX(entity.getRotationX())
                .rotationY(entity.getRotationY())
                .rotationZ(entity.getRotationZ())
                .eyeOffset(entity.getEyeOffset())
                .bridgeOffset(entity.getBridgeOffset())
                .templeOffset(entity.getTempleOffset())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .validatedAt(entity.getValidatedAt() != null ? entity.getValidatedAt() : null)
                .validatedBy(entity.getValidatedBy())
                .build();
    }
}
