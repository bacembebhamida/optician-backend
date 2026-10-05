package com.optician.backend.repository;

import com.optician.backend.model.VirtualTryOnAsset;
import com.optician.backend.model.enums.TryOnAssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VirtualTryOnAssetRepository extends JpaRepository<VirtualTryOnAsset, Long> {

    List<VirtualTryOnAsset> findByVariantId(Long variantId);

    Optional<VirtualTryOnAsset> findFirstByVariantIdAndStatus(Long variantId, TryOnAssetStatus status);

    Optional<VirtualTryOnAsset> findFirstByVariantIdOrderByVersionDesc(Long variantId);
}
