package com.optician.backend.service;

import com.optician.backend.dto.ProductRequestDto;
import com.optician.backend.dto.ProductResponseDto;
import com.optician.backend.dto.ProductSearchFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto dto);
    ProductResponseDto getProductById(Long id);
    List<ProductResponseDto> getAllProducts();
    Page<ProductResponseDto> searchProducts(ProductSearchFilter filter, Pageable pageable);
    ProductResponseDto updateProduct(Long id, ProductRequestDto dto);
    ProductResponseDto patchProduct(Long id, Map<String, Object> updates);
    void deleteProduct(Long id);
    ProductResponseDto toggleProductActive(Long id, boolean active);
}
