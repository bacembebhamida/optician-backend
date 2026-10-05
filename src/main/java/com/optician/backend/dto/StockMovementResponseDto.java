package com.optician.backend.dto;

import com.optician.backend.model.enums.StockMovementType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementResponseDto {

    private Long id;

    private Long productVariantId;
    private String variantSku;
    private String productName;

    private Long storeId;
    private String storeName;

    private StockMovementType type;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;

    private String reference;
    private String reason;
    private String user;
    private LocalDateTime createdAt;
}
