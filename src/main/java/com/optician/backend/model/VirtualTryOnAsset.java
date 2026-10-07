package com.optician.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optician.backend.model.enums.TryOnAssetStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité représentant un actif 3D d'essayage virtuel (fichier GLB/glTF)
 * associé à une variante de produit.
 */
@Entity
@Table(name = "virtual_try_on_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualTryOnAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonIgnore
    private ProductVariant variant;

    @Column(nullable = false)
    private String modelUrl;

    private String thumbnailUrl;

    @Builder.Default
    @Column(length = 20)
    private String format = "GLB";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TryOnAssetStatus status = TryOnAssetStatus.DRAFT;

    @Builder.Default
    private Integer version = 1;

    private String jobId;

    // Quality Scoring System (0 - 100)
    @Builder.Default
    private Integer qualityScore = 85;
    @Builder.Default
    private Integer geometryScore = 88;
    @Builder.Default
    private Integer symmetryScore = 95;
    @Builder.Default
    private Integer scaleScore = 90;
    @Builder.Default
    private Integer materialScore = 85;

    @Column(length = 2000)
    private String statusDetails;

    // Visual Comparison Test Renders & Similarity
    @Builder.Default
    private Integer visualSimilarityScore = 88;
    private String renderFrontUrl;
    private String renderThreeQuarterUrl;
    private String renderSideUrl;
    private String renderBackUrl;

    // Optical Dimensions (mm)
    private Integer opticalLensWidth;
    private Integer opticalBridgeWidth;
    private Integer opticalTempleLength;
    private Integer opticalTotalWidth;
    private Integer opticalLensHeight;

    // Paramètres de calibration 3D
    @Builder.Default
    private Double scale = 1.0;

    @Builder.Default
    private Double positionX = 0.0;

    @Builder.Default
    private Double positionY = 0.0;

    @Builder.Default
    private Double positionZ = 0.0;

    @Builder.Default
    private Double rotationX = 0.0;

    @Builder.Default
    private Double rotationY = 0.0;

    @Builder.Default
    private Double rotationZ = 0.0;

    @Builder.Default
    private Double eyeOffset = 0.0;

    @Builder.Default
    private Double bridgeOffset = 0.0;

    @Builder.Default
    private Double templeOffset = 0.0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime validatedAt;
    private String validatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
