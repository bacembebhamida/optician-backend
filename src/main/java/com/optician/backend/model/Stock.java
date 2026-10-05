package com.optician.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stocks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stock_variant_store", columnNames = {"product_variant_id", "store_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    @Builder.Default
    private Integer minimumStock = 0;

    @Builder.Default
    private Integer maximumStock = 100;

    @Builder.Default
    private Integer reorderPoint = 5;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getAvailableQuantity() {
        int q = quantity != null ? quantity : 0;
        int r = reservedQuantity != null ? reservedQuantity : 0;
        return Math.max(0, q - r);
    }

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
