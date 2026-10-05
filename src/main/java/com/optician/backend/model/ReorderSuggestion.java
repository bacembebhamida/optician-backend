package com.optician.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Suggestion de réapprovisionnement générée automatiquement par le système.
 *
 * Règle métier :
 *   Si Stock.quantity <= Stock.reorderPoint → GenèreReorderSuggestion
 *   Quantité suggérée = Stock.maximumStock - Stock.quantity
 *
 * Aucune commande fournisseur n'est créée automatiquement sans validation humaine.
 */
@Entity
@Table(name = "reorder_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private Integer currentQuantity;

    @Column(nullable = false)
    private Integer reorderPoint;

    @Column(nullable = false)
    private Integer maximumStock;

    /** Quantité suggérée = maximumStock - currentQuantity */
    @Column(nullable = false)
    private Integer suggestedQuantity;

    /** Prix d'achat estimé unitaire */
    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedUnitPrice;

    /** Délai de livraison estimé (jours) */
    private Integer estimatedDeliveryDays;

    /**
     * Statut de la suggestion :
     * PENDING    → générée, en attente de traitement
     * APPROVED   → validée par un responsable
     * ORDERED    → commande fournisseur créée
     * DISMISSED  → rejetée
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    private String generatedBy;
    private String processedBy;

    @Column(length = 1000)
    private String notes;

    private LocalDateTime generatedAt;
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.generatedAt = LocalDateTime.now();
    }
}
