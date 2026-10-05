package com.optician.backend.dto;

import com.optician.backend.model.enums.InventoryStatus;
import com.optician.backend.model.enums.InventoryType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInventoryResponseDto {

    private Long id;
    private String inventoryReference;

    private Long storeId;
    private String storeName;

    private InventoryType type;
    private InventoryStatus status;

    private String performedBy;
    private String validatedBy;
    private String notes;

    private List<ItemDto> items;
    private Integer totalItems;
    private Integer totalDiscrepancy;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDto {
        private Long id;
        private Long productVariantId;
        private String variantSku;
        private String productName;
        private Integer theoreticalQuantity;
        private Integer physicalQuantity;
        private Integer discrepancyQuantity;
        private Boolean adjusted;
        private String notes;
    }
}
