package com.optician.backend.model;

import com.optician.backend.model.enums.InventoryStatus;
import com.optician.backend.model.enums.InventoryType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String inventoryReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InventoryType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InventoryStatus status;

    private String performedBy;
    private String validatedBy;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "stockInventory", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockInventoryItem> items = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
