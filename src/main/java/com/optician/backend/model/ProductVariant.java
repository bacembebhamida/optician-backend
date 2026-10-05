package com.optician.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Variante d'un produit (couleur, taille...).
 *
 * IMPORTANT : Le stock n'est PAS stocké dans cette entité.
 * Chaque variante dispose d'un enregistrement Stock par magasin (table stocks).
 *
 * Le champ legacyStock est conservé uniquement pour la compatibilité
 * avec les données de démonstration du DataInitializer.
 * Il ne doit PAS être utilisé dans les nouvelles logiques métier.
 */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(unique = true)
    private String barcode;

    private String color;

    private String size;

    /** Référence fournisseur spécifique à cette variante */
    private String supplierReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * @deprecated Utiliser la table stocks à la place.
     * Conservé pour rétro-compatibilité avec DataInitializer.
     */
    @Deprecated
    @Column(name = "legacy_stock")
    @Builder.Default
    private Integer stock = 0;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<VirtualTryOnAsset> tryOnAssets = new ArrayList<>();

    /** Stocks par magasin - relation inverse */
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Stock> stocks = new ArrayList<>();

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
