package com.optician.backend.repository;

import com.optician.backend.model.StockReservation;
import com.optician.backend.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByOrderReferenceAndStatus(String orderReference, ReservationStatus status);

    List<StockReservation> findByStockIdAndStatus(Long stockId, ReservationStatus status);

    List<StockReservation> findByOrderReference(String orderReference);

    /** Réservations expirées encore actives */
    @Query("SELECT r FROM StockReservation r WHERE r.status = 'ACTIVE' AND r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    List<StockReservation> findExpiredReservations(@Param("now") LocalDateTime now);
}
