package com.optician.backend.repository;

import com.optician.backend.model.StockInventory;
import com.optician.backend.model.enums.InventoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockInventoryRepository extends JpaRepository<StockInventory, Long>, JpaSpecificationExecutor<StockInventory> {

    Optional<StockInventory> findByInventoryReference(String inventoryReference);

    boolean existsByInventoryReference(String inventoryReference);

    Page<StockInventory> findByStoreId(Long storeId, Pageable pageable);

    Page<StockInventory> findByStatus(InventoryStatus status, Pageable pageable);

    Page<StockInventory> findByStoreIdAndStatus(Long storeId, InventoryStatus status, Pageable pageable);
}
