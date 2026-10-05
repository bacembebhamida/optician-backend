package com.optician.backend.service;

import com.optician.backend.dto.ProductVariantRequestDto;
import com.optician.backend.dto.ProductVariantResponseDto;

import java.util.List;

public interface ProductVariantService {
    ProductVariantResponseDto addVariant(Long productId, ProductVariantRequestDto dto);
    List<ProductVariantResponseDto> getVariantsByProductId(Long productId);
    ProductVariantResponseDto updateVariant(Long variantId, ProductVariantRequestDto dto);
    void deleteVariant(Long variantId);
}
