package com.optician.backend.repository;

import com.optician.backend.model.StockTransfer;
import com.optician.backend.model.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long>, JpaSpecificationExecutor<StockTransfer> {

    Optional<StockTransfer> findByTransferReference(String transferReference);

    boolean existsByTransferReference(String transferReference);

    Page<StockTransfer> findBySourceStoreId(Long storeId, Pageable pageable);

    Page<StockTransfer> findByTargetStoreId(Long storeId, Pageable pageable);

    Page<StockTransfer> findByStatus(TransferStatus status, Pageable pageable);
}
