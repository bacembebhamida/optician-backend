package com.optician.backend.dto;

import com.optician.backend.model.enums.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchFilter {

    private String query;
    private String sku;
    private String barcode;
    private Long brandId;
    private String brand;
    private Long categoryId;
    private String category;
    private ProductType productType;
    private String color;
    private String material;
    private Gender gender;
    private FrameShape frameShape;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean active;
    private Boolean featured;
}
