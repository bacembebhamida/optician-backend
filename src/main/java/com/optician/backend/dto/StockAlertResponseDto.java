package com.optician.backend.dto;

import com.optician.backend.model.enums.StockAlertStatus;
import com.optician.backend.model.enums.StockAlertType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlertResponseDto {

    private Long id;

    private Long stockId;
    private Long productVariantId;
    private String variantSku;
    private String productName;

    private Long storeId;
    private String storeName;

    private Integer currentQuantity;
    private Integer reorderPoint;

    private StockAlertType alertType;
    private StockAlertStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
