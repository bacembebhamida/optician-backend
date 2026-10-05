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

        ProductVariant saved = variantRepository.save(variant);

        auditService.logAudit(AuditAction.CREATE, "ProductVariant", saved.getId(), null, "Created variant SKU " + saved.getSku());

        return variantMapper.toDto(saved);
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
