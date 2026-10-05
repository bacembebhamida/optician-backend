package com.optician.backend.repository;

import com.optician.backend.model.StockAlert;
import com.optician.backend.model.enums.StockAlertStatus;
import com.optician.backend.model.enums.StockAlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    Page<StockAlert> findByStatus(StockAlertStatus status, Pageable pageable);

    Page<StockAlert> findByAlertType(StockAlertType alertType, Pageable pageable);

    @Query("SELECT a FROM StockAlert a WHERE a.stock.id = :stockId AND a.alertType = :alertType AND a.status = 'OPEN'")
    Optional<StockAlert> findOpenAlertForStockAndType(
            @Param("stockId") Long stockId,
            @Param("alertType") StockAlertType alertType);

    @Query("SELECT a FROM StockAlert a WHERE a.stock.store.id = :storeId AND a.status = 'OPEN'")
    Page<StockAlert> findOpenAlertsByStore(@Param("storeId") Long storeId, Pageable pageable);
}
