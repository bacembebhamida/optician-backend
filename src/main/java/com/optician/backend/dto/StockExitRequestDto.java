package com.optician.backend.dto;

import com.optician.backend.model.enums.StockMovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockExitRequestDto {

    @NotNull(message = "Product variant ID is mandatory")
    private Long productVariantId;

    @NotNull(message = "Store ID is mandatory")
    private Long storeId;

    @NotNull(message = "Quantity is mandatory")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Movement type is mandatory")
    private StockMovementType type;

    private String reference;
    private String reason;
}
