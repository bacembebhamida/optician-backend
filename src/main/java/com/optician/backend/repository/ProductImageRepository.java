package com.optician.backend.repository;

import com.optician.backend.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);
    List<ProductImage> findByVariantIdOrderByDisplayOrderAsc(Long variantId);
}
