package com.optician.backend.repository;

import com.optician.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByReference(String reference);

    java.util.Optional<Product> findByReference(String reference);

    long countByBrandEntityId(Long brandId);

    long countByCategoryEntityId(Long categoryId);

    // Legacy query methods kept for full backward compatibility
    @Query("SELECT p FROM Product p WHERE LOWER(p.categoryEntity.name) = LOWER(:category) OR LOWER(p.legacyCategory) = LOWER(:category)")
    List<Product> findByCategory(@Param("category") String category);

    @Query("SELECT p FROM Product p WHERE LOWER(p.brandEntity.name) = LOWER(:brand) OR LOWER(p.legacyBrand) = LOWER(:brand)")
    List<Product> findByBrand(@Param("brand") String brand);

    @Query("SELECT p FROM Product p WHERE LOWER(CAST(p.frameShape AS string)) = LOWER(:faceShape)")
    List<Product> findByFaceShape(@Param("faceShape") String faceShape);
}
