package com.optician.backend.repository;

import com.optician.backend.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {

    Optional<Stock> findByProductVariantIdAndStoreId(Long variantId, Long storeId);

    /** Pour modification concurrente sécurisée : verrou pessimiste */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.productVariant.id = :variantId AND s.store.id = :storeId")
    Optional<Stock> findByVariantAndStoreForUpdate(
            @Param("variantId") Long variantId,
            @Param("storeId") Long storeId);

    List<Stock> findByProductVariantId(Long variantId);

    List<Stock> findByStoreId(Long storeId);

    @Query("SELECT s FROM Stock s WHERE s.productVariant.product.id = :productId")
    List<Stock> findByProductId(@Param("productId") Long productId);

    /** Stocks en rupture : quantité disponible <= 0 */
    @Query("SELECT s FROM Stock s WHERE (s.quantity - s.reservedQuantity) <= 0")
    List<Stock> findOutOfStockAll();

    /** Stocks en rupture pour un magasin */
    @Query("SELECT s FROM Stock s WHERE s.store.id = :storeId AND (s.quantity - s.reservedQuantity) <= 0")
    List<Stock> findOutOfStockByStore(@Param("storeId") Long storeId);

    /** Stocks sous le seuil de réapprovisionnement */
    @Query("SELECT s FROM Stock s WHERE s.quantity <= s.reorderPoint AND s.quantity > 0")
    List<Stock> findLowStockAll();

    /** Stocks en surstock */
    @Query("SELECT s FROM Stock s WHERE s.maximumStock IS NOT NULL AND s.quantity > s.maximumStock")
    List<Stock> findOverStockAll();
}
