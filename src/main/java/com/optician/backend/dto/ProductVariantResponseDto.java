package com.optician.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponseDto {

    private Long id;
    private Long productId;
    private String sku;
    private String barcode;
    private String color;
    private String size;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private Boolean active;
    private Integer stock;
    private List<ProductImageDto> images;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
