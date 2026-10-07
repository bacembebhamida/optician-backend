package com.optician.backend.service.impl;

import com.optician.backend.dto.ProductVariantRequestDto;
import com.optician.backend.dto.ProductVariantResponseDto;
import com.optician.backend.exception.DuplicateResourceException;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.mapper.ProductVariantMapper;
import com.optician.backend.model.Product;
import com.optician.backend.model.ProductVariant;
import com.optician.backend.model.enums.AuditAction;
import com.optician.backend.repository.ProductRepository;
import com.optician.backend.repository.ProductVariantRepository;
import com.optician.backend.service.ProductAuditService;
import com.optician.backend.service.ProductVariantService;
import com.optician.backend.model.VirtualTryOnAsset;
import com.optician.backend.model.enums.TryOnAssetStatus;
import com.optician.backend.repository.VirtualTryOnAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final VirtualTryOnAssetRepository assetRepository;
    private final ProductVariantMapper variantMapper;
    private final ProductAuditService auditService;

    @Override
    @Transactional
    public ProductVariantResponseDto addVariant(Long productId, ProductVariantRequestDto dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (variantRepository.existsBySku(dto.getSku())) {
            throw new DuplicateResourceException("Variant SKU already exists: " + dto.getSku());
        }

        if (dto.getBarcode() != null && !dto.getBarcode().trim().isEmpty() && variantRepository.existsByBarcode(dto.getBarcode())) {
            throw new DuplicateResourceException("Variant Barcode already exists: " + dto.getBarcode());
        }

        ProductVariant variant = variantMapper.toEntity(dto);
        variant.setProduct(product);

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            for (com.optician.backend.dto.ProductImageDto imgDto : dto.getImages()) {
                if (imgDto.getUrl() == null || imgDto.getUrl().isBlank()) continue;
                com.optician.backend.model.ProductImage variantImg = com.optician.backend.model.ProductImage.builder()
                        .variant(variant)
                        .url(imgDto.getUrl())
                        .altText(imgDto.getAltText())
                        .displayOrder(imgDto.getDisplayOrder() != null ? imgDto.getDisplayOrder() : 0)
                        .primaryImage(imgDto.getPrimaryImage() != null ? imgDto.getPrimaryImage() : false)
                        .build();
                variant.getImages().add(variantImg);
            }
        }

        ProductVariant saved = variantRepository.save(variant);

        autoGenerate3dAssetForVariant(product, saved);

        auditService.logAudit(AuditAction.CREATE, "ProductVariant", saved.getId(), null, "Created variant SKU " + saved.getSku());

        return variantMapper.toDto(saved);
    }

    private void autoGenerate3dAssetForVariant(Product product, ProductVariant variant) {
        if (product == null || variant == null || variant.getId() == null) return;
        if (assetRepository == null) return;

        boolean hasPublished = assetRepository.findFirstByVariantIdAndStatus(variant.getId(), TryOnAssetStatus.PUBLISHED).isPresent();
        if (!hasPublished) {
            String shape = product.getFrameShape() != null ? product.getFrameShape().name().toLowerCase() : "rectangle";
            String modelUrl = product.getModel3dUrl() != null && !product.getModel3dUrl().isBlank()
                    ? product.getModel3dUrl()
                    : "/uploads/models/procedural_" + shape + ".glb";

            VirtualTryOnAsset asset = VirtualTryOnAsset.builder()
                    .variant(variant)
                    .modelUrl(modelUrl)
                    .thumbnailUrl(product.getImageUrl())
                    .format("GLB")
                    .status(TryOnAssetStatus.PUBLISHED)
                    .version(1)
                    .scale(1.0)
                    .positionX(0.0)
                    .positionY(0.0)
                    .positionZ(0.0)
                    .rotationX(0.0)
                    .rotationY(0.0)
                    .rotationZ(0.0)
                    .build();

            assetRepository.save(asset);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponseDto> getVariantsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return variantRepository.findByProductId(productId).stream()
                .map(variantMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ProductVariantResponseDto updateVariant(Long variantId, ProductVariantRequestDto dto) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (dto.getSku() != null && !dto.getSku().equals(variant.getSku()) && variantRepository.existsBySku(dto.getSku())) {
            throw new DuplicateResourceException("Variant SKU already exists: " + dto.getSku());
        }

        String oldVal = "SKU: " + variant.getSku() + ", Price: " + variant.getSellingPrice();

        if (dto.getSku() != null) variant.setSku(dto.getSku());
        if (dto.getBarcode() != null) variant.setBarcode(dto.getBarcode());
        if (dto.getColor() != null) variant.setColor(dto.getColor());
        if (dto.getSize() != null) variant.setSize(dto.getSize());
        if (dto.getPurchasePrice() != null) variant.setPurchasePrice(dto.getPurchasePrice());
        if (dto.getSellingPrice() != null) variant.setSellingPrice(dto.getSellingPrice());
        if (dto.getActive() != null) variant.setActive(dto.getActive());
        if (dto.getStock() != null) variant.setStock(dto.getStock());

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            variant.getImages().clear();
            for (com.optician.backend.dto.ProductImageDto imgDto : dto.getImages()) {
                if (imgDto.getUrl() == null || imgDto.getUrl().isBlank()) continue;
                com.optician.backend.model.ProductImage variantImg = com.optician.backend.model.ProductImage.builder()
                        .variant(variant)
                        .url(imgDto.getUrl())
                        .altText(imgDto.getAltText())
                        .displayOrder(imgDto.getDisplayOrder() != null ? imgDto.getDisplayOrder() : 0)
                        .primaryImage(imgDto.getPrimaryImage() != null ? imgDto.getPrimaryImage() : false)
                        .build();
                variant.getImages().add(variantImg);
            }
        }

        ProductVariant saved = variantRepository.save(variant);

        auditService.logAudit(AuditAction.UPDATE, "ProductVariant", saved.getId(), oldVal, "Updated variant SKU " + saved.getSku());

        return variantMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        auditService.logAudit(AuditAction.DELETE, "ProductVariant", variantId, "SKU: " + variant.getSku(), "Deleted");
        variantRepository.delete(variant);
    }
}
