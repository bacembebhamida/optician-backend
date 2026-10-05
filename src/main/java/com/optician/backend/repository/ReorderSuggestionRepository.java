package com.optician.backend.repository;

import com.optician.backend.model.ReorderSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {

    Page<ReorderSuggestion> findByStatus(String status, Pageable pageable);

    Page<ReorderSuggestion> findByStoreId(Long storeId, Pageable pageable);

    @Query("SELECT r FROM ReorderSuggestion r WHERE r.status = 'PENDING' AND r.store.id = :storeId")
    Page<ReorderSuggestion> findPendingByStore(@Param("storeId") Long storeId, Pageable pageable);

    @Query("SELECT r FROM ReorderSuggestion r WHERE r.productVariant.id = :variantId AND r.store.id = :storeId AND r.status = 'PENDING'")
    boolean existsPendingForVariantAndStore(@Param("variantId") Long variantId, @Param("storeId") Long storeId);
}
