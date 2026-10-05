package com.optician.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationRequestDto {

    @NotNull(message = "Product variant ID is mandatory")
    private Long productVariantId;

    @NotNull(message = "Store ID is mandatory")
    private Long storeId;

    @NotBlank(message = "Order reference is mandatory")
    private String orderReference;

    @NotNull(message = "Quantity is mandatory")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Builder.Default
    private Integer expirationMinutes = 60;
}
