package com.optician.backend.repository;

import com.optician.backend.model.StockMovement;
import com.optician.backend.model.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    Page<StockMovement> findByProductVariantId(Long variantId, Pageable pageable);

    Page<StockMovement> findByStoreId(Long storeId, Pageable pageable);

    Page<StockMovement> findByType(StockMovementType type, Pageable pageable);

    @Query("SELECT m FROM StockMovement m WHERE m.productVariant.product.id = :productId ORDER BY m.createdAt DESC")
    Page<StockMovement> findByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT m FROM StockMovement m WHERE m.productVariant.id = :variantId AND m.store.id = :storeId ORDER BY m.createdAt DESC")
    List<StockMovement> findByVariantAndStore(@Param("variantId") Long variantId, @Param("storeId") Long storeId);

    @Query("SELECT m FROM StockMovement m WHERE m.createdAt BETWEEN :from AND :to ORDER BY m.createdAt DESC")
    List<StockMovement> findBetweenDates(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
