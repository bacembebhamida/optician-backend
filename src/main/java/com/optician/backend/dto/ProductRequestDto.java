package com.optician.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optician.backend.model.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO de création / mise à jour d'un Product (MODÈLE COMMERCIAL).
 *
 * Accepte à la fois la structure canonique et les alias du frontend Angular
 * (sku, type, shape, optical, etc.).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    /** Référence commerciale du modèle. Optionnelle : auto-générée si absente. */
    @JsonAlias({"sku", "reference"})
    @Size(max = 255, message = "Reference must not exceed 255 characters")
    private String reference;

    @NotBlank(message = "Product name is mandatory")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Short description must not exceed 255 characters")
    private String shortDescription;

    @Size(max = 4000, message = "Description must not exceed 4000 characters")
    private String description;

    @NotNull(message = "Product type is mandatory")
    @JsonAlias({"type", "productType"})
    private ProductType productType;

    private Long categoryId;
    private Long subCategoryId;
    private Long brandId;

    @Size(max = 255, message = "Model must not exceed 255 characters")
    private String model;

    private Gender gender;
    private TargetAge targetAge;

    @Size(max = 255, message = "Material must not exceed 255 characters")
    private String material;

    @Size(max = 255, message = "Collection must not exceed 255 characters")
    private String collection;

    /** @deprecated Fournisseur historique au niveau modèle ; utiliser les variantes. */
    @Deprecated
    private String supplier;

    @JsonAlias({"shape", "frameShape"})
    private FrameShape frameShape;

    @Builder.Default
    private Boolean active = true;

    private ProductStatus status;

    @Builder.Default
    private Boolean featured = false;

    @Builder.Default
    private Boolean searchable = true;

    @Builder.Default
    private Boolean tryOn3dAvailable = true;

    private String model3dUrl;

    private String model3dConfig;

    /** @deprecated Ancien drapeau de visualisation 3D ; conservé pour compatibilité. */
    @Deprecated
    @Builder.Default
    private Boolean virtualTryOnEnabled = true;

    /** @deprecated Ancienne URL d'image produit ; préférer la liste d'images. */
    @Deprecated
    private String imageUrl;

    /** Création imbriquée optionnelle des variantes (sinon : POST /api/products/{id}/variants). */
    @Valid
    private List<ProductVariantRequestDto> variants;

    private List<ProductImageDto> images;

    @JsonProperty("optical")
    public void setOptical(Map<String, Object> optical) {
        if (optical != null) {
            if (this.frameShape == null && optical.get("shape") != null) {
                this.frameShape = FrameShape.fromString(String.valueOf(optical.get("shape")));
            }
            if (this.material == null && optical.get("material") != null) {
                this.material = String.valueOf(optical.get("material"));
            }
        }
    }

    @JsonProperty("category")
    public void setCategory(Object cat) {
        if (cat != null && this.categoryId == null) {
            try {
                this.categoryId = Long.parseLong(String.valueOf(cat));
            } catch (NumberFormatException ignored) {}
        }
    }

    @JsonProperty("brandId")
    public void setBrandId(Object bId) {
        if (bId != null) {
            try {
                this.brandId = Long.parseLong(String.valueOf(bId));
            } catch (NumberFormatException ignored) {}
        }
    }
}

