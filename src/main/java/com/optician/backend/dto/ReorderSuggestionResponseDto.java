package com.optician.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderSuggestionResponseDto {

    private Long id;

    private Long productVariantId;
    private String variantSku;
    private String productName;

    private Long storeId;
    private String storeName;

    private Long supplierId;
    private String supplierName;

    private Integer currentQuantity;
    private Integer reorderPoint;
    private Integer maximumStock;
    private Integer suggestedQuantity;

    private BigDecimal estimatedUnitPrice;
    private BigDecimal estimatedTotalCost;
    private Integer estimatedDeliveryDays;

    private String status;
    private String generatedBy;
    private String processedBy;
    private String notes;

    private LocalDateTime generatedAt;
    private LocalDateTime processedAt;
}
