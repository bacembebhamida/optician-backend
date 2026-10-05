package com.optician.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponseDto {

    private Long id;

    private Long productVariantId;
    private String variantSku;
    private String variantColor;
    private String variantSize;

    private Long productId;
    private String productName;
    private String brandName;
    private String categoryName;

    private Long storeId;
    private String storeName;
    private String storeCity;

    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer minimumStock;
    private Integer maximumStock;
    private Integer reorderPoint;

    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private Boolean isOverStock;

    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
