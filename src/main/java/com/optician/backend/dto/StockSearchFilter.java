package com.optician.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSearchFilter {

    private Long productId;
    private Long productVariantId;
    private Long storeId;
    private String sku;
    private String barcode;
    private String brand;
    private String category;
    private Boolean lowStockOnly;
    private Boolean outOfStockOnly;
    private Boolean overStockOnly;
    private Boolean availableOnly;
    private String query;
}
