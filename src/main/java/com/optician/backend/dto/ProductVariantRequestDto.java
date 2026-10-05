package com.optician.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequestDto {

    @NotBlank(message = "Variant SKU is mandatory")
    private String sku;

    private String barcode;
    private String color;
    private String size;

    @DecimalMin(value = "0.0", message = "Purchase price must be greater than or equal to zero")
    private BigDecimal purchasePrice;

    @DecimalMin(value = "0.0", message = "Selling price must be greater than or equal to zero")
    private BigDecimal sellingPrice;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Integer stock = 0;

    private List<ProductImageDto> images;
}
