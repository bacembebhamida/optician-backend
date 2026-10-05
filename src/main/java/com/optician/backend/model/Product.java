package com.optician.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.optician.backend.model.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product représente le MODÈLE COMMERCIAL d'une référence optique (ex : Ray-Ban RX5228).
 *
 * RÈGLE D'ARCHITECTURE :
 * Aucune donnée spécifique à une déclinaison vendable ne doit être portée ici :
 *   - SKU et barcode variante  → {@link ProductVariant}
 *   - couleur, dimensions, prix → {@link ProductVariant}
 *   - stock / quantité par magasin → {@link Stock} (variante + magasin)
 *
 * Les méthodes getPrice() et getStock() sont des AGREGATS DÉRIVÉS fournis
 * uniquement pour les vues de catalogue (prix à partir de / stock total) :
 * elles ne stockent aucune donnée.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Référence commerciale du modèle (ex : RX5228). Auto-générée si absente. */
    @Column(name = "reference")
    private String reference;

    @Column(nullable = false)
    private String name;

    private String shortDescription;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"subCategories", "parent"})
    private Category categoryEntity;

    /** @deprecated Colonne de compatibilité (ancien stockage texte de la catégorie). Préférer categoryEntity. */
    @Deprecated
    @Column(name = "legacy_category")
    private String legacyCategory;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sub_category_id")
    @JsonIgnoreProperties({"subCategories", "parent"})
    private Category subCategory;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id")
    private Brand brandEntity;

    /** @deprecated Colonne de compatibilité (ancien stockage texte de la marque). Préférer brandEntity. */
    @Deprecated
    @Column(name = "legacy_brand")
    private String legacyBrand;

    private String model;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private TargetAge targetAge;

    private String material;

    private String collection;

    /** @deprecated Fournisseur historique au niveau modèle ; le fournisseur d'une déclinaison est sur ProductVariant. */
    @Deprecated
    private String supplier;

    @Enumerated(EnumType.STRING)
    private FrameShape frameShape;

    @Builder.Default
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIF;

    @Builder.Default
    private Boolean featured = false;

    @Builder.Default
    private Boolean searchable = true;

    /** Drapeau indiquant si la monture est disponible pour l'essayage 3D en réalité augmentée. */
    @Builder.Default
    private Boolean tryOn3dAvailable = true;

    /** URL ou chemin du fichier de modèle 3D (.glb / .gltf). */
    @Column(name = "model_3d_url", length = 1000)
    private String model3dUrl;

    /** Configuration JSON du modèle 3D généré (dimensions, maillage, teintes, réfraction). */
    @Column(name = "model_3d_config", length = 4000)
    private String model3dConfig;

    /** @deprecated Ancien drapeau de visualisation 3D ; conservé pour compatibilité. */
    @Deprecated
    @Builder.Default
    private Boolean virtualTryOnEnabled = true;

    /** @deprecated Ancienne URL d'image au niveau produit ; utiliser la relation ProductImage. */
    @Deprecated
    @Column(length = 1000)
    private String imageUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.reference == null || this.reference.isBlank()) {
            this.reference = "REF-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // Agrégats dérivés (aucune donnée stockée dans Product)
    // =========================================================

    /** Stock total du modèle = somme des stocks réels par magasin de toutes ses variantes (table stocks). */
    public Integer getStock() {
        if (variants == null || variants.isEmpty()) {
            return 0;
        }
        return variants.stream()
                .filter(v -> v.getStocks() != null)
                .flatMap(v -> v.getStocks().stream())
                .mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0)
                .sum();
    }

    /** Prix de vente minimal parmi les variantes actives (null si aucune variante). */
    public Double getPrice() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                .map(ProductVariant::getSellingPrice)
                .filter(p -> p != null)
                .min(BigDecimal::compareTo)
                .map(BigDecimal::doubleValue)
                .orElse(null);
    }

    /** Nombre de variantes (vues « X variantes »). */
    public long getVariantCount() {
        return variants != null ? variants.size() : 0L;
    }

    // =========================================================
    // Getters/Setters de compatibilité (frontend hérité)
    // =========================================================

    @Deprecated
    public String getCategory() {
        if (categoryEntity != null) {
            return categoryEntity.getName();
        }
        return legacyCategory;
    }

    @Deprecated
    public void setCategory(String category) {
        this.legacyCategory = category;
    }

    @Deprecated
    public String getBrand() {
        if (brandEntity != null) {
            return brandEntity.getName();
        }
        return legacyBrand;
    }

    @Deprecated
    public void setBrand(String brand) {
        this.legacyBrand = brand;
    }

    @Deprecated
    public String getFaceShape() {
        return frameShape != null ? frameShape.name() : null;
    }

    @Deprecated
    public void setFaceShape(String faceShape) {
        // Le type de face est maintenant géré par l'enum FrameShape.
    }

    @Deprecated
    public String getFrameType() {
        return material;
    }

    @Deprecated
    public void setFrameType(String frameType) {
        this.material = frameType;
    }

    public static class ProductBuilder {
        @Deprecated
        public ProductBuilder brand(String brand) {
            this.legacyBrand = brand;
            return this;
        }

        @Deprecated
        public ProductBuilder category(String category) {
            this.legacyCategory = category;
            return this;
        }
    }
}
