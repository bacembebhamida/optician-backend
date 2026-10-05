package com.optician.backend.dto;

import com.optician.backend.model.enums.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse d'un Product (MODÈLE COMMERCIAL).
 *
 * Les champs {@code price} (prix à partir de), {@code stock} (stock total) et
 * {@code variantCount} sont des valeurs DÉRIVÉES calculées depuis les variantes
 * et la table stocks. Aucune donnée de déclinaison n'est stockée dans Product.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {

    private Long id;
    private String reference;
    private String name;
    private String shortDescription;
    private String description;
    private ProductType productType;

    private Long categoryId;
    private String categoryName;
    private String category;
    private Long subCategoryId;
    private String subCategoryName;
    private Long brandId;
    private String brandName;
    private String brand;

    private String model;
    private Gender gender;
    private TargetAge targetAge;
    private String material;
    private String collection;

    /** @deprecated Fournisseur historique au niveau modèle ; géré par les variantes. */
    @Deprecated
    private String supplier;

    private FrameShape frameShape;
    private Boolean active;
    private ProductStatus status;
    private Boolean featured;
    private Boolean searchable;

    private Boolean tryOn3dAvailable;
    private String model3dUrl;
    private String model3dConfig;

    /** @deprecated Ancien drapeau de visualisation 3D ; conservé pour compatibilité. */
    @Deprecated
    private Boolean virtualTryOnEnabled;

    /** @deprecated Ancienne URL d'image produit ; préférer la liste d'images. */
    @Deprecated
    private String imageUrl;

    /** Prix de vente minimal des variantes actives — valeur dérivée ("à partir de"). */
    private Double price;

    /** Stock total toutes variantes et tous magasins — valeur dérivée. */
    private Integer stock;

    /** Nombre de variantes — valeur dérivée. */
    private Long variantCount;

    private List<ProductVariantResponseDto> variants;
    private List<ProductImageDto> images;

    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
