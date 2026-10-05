package com.optician.backend.repository;

import com.optician.backend.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByActiveTrue();
    List<Store> findByCityIgnoreCase(String city);
}
