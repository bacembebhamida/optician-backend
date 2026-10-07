package com.optician.backend.dto;

import com.optician.backend.model.enums.TryOnAssetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualTryOnAssetResponseDto {

    private Long id;
    private Long variantId;
    private String variantSku;
    private String modelUrl;
    private String thumbnailUrl;
    private String format;
    private TryOnAssetStatus status;
    private Integer version;

    private String jobId;

    // Quality Score System (0-100)
    private Integer qualityScore;
    private Integer geometryScore;
    private Integer symmetryScore;
    private Integer scaleScore;
    private Integer materialScore;
    private Integer visualSimilarityScore;
    private String renderFrontUrl;
    private String renderThreeQuarterUrl;
    private String renderSideUrl;
    private String renderBackUrl;
    private String statusDetails;

    // Optical Dimensions (mm)
    private Integer opticalLensWidth;
    private Integer opticalBridgeWidth;
    private Integer opticalTempleLength;
    private Integer opticalTotalWidth;
    private Integer opticalLensHeight;

    // Calibration 3D
    private Double scale;
    private Double positionX;
    private Double positionY;
    private Double positionZ;
    private Double rotationX;
    private Double rotationY;
    private Double rotationZ;
    private Double eyeOffset;
    private Double bridgeOffset;
    private Double templeOffset;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime validatedAt;
    private String validatedBy;
}
