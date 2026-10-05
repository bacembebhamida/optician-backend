package com.optician.backend.service.impl;

import com.optician.backend.dto.*;
import com.optician.backend.exception.*;
import com.optician.backend.mapper.ProductMapper;
import com.optician.backend.mapper.ProductVariantMapper;
import com.optician.backend.model.Brand;
import com.optician.backend.model.Category;
import com.optician.backend.model.Product;
import com.optician.backend.model.ProductVariant;
import com.optician.backend.model.enums.AuditAction;
import com.optician.backend.model.enums.FrameShape;
import com.optician.backend.model.enums.Gender;
import com.optician.backend.model.enums.ProductStatus;
import com.optician.backend.model.enums.TargetAge;
import com.optician.backend.repository.*;
import com.optician.backend.service.ProductAuditService;
import com.optician.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Implémentation du service Product (MODÈLE COMMERCIAL).
 *
 * RÈGLE : Product ne porte aucune donnée de déclinaison (SKU, barcode, couleur,
 * prix, stock). Ces données sont gérées par ProductVariant et par le StockService
 * (table stocks). Les agrégats price/stock exposés sont calculés à la volée.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final VirtualTryOnAssetRepository assetRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper variantMapper;
    private final ProductAuditService auditService;

    @Override
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto dto) {
        String targetRef = dto.getReference() != null && !dto.getReference().trim().isEmpty()
                ? dto.getReference().trim()
                : "REF-" + System.currentTimeMillis();

        if (productRepository.existsByReference(targetRef)) {
            throw new DuplicateResourceException("Product with reference '" + targetRef + "' already exists");
        }

        Product product = productMapper.toEntity(dto);
        product.setReference(targetRef);
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
            product.setActive(dto.getStatus() == ProductStatus.ACTIF);
        }

        // Auto-enable 3D try-on for product
        product.setTryOn3dAvailable(true);
        product.setVirtualTryOnEnabled(true);
        if (product.getFrameShape() == null) {
            product.setFrameShape(FrameShape.RECTANGULAIRE);
        }
        if (product.getModel3dConfig() == null || product.getModel3dConfig().isBlank()) {
            product.setModel3dConfig("{\"frameShape\":\"" + product.getFrameShape().name() + "\",\"material\":\"" + (product.getMaterial() != null ? product.getMaterial() : "ACETATE") + "\",\"scale\":1.0}");
        }

        resolveBrandAndCategory(dto, product);

        if (dto.getVariants() != null && !dto.getVariants().isEmpty()) {
            for (ProductVariantRequestDto vDto : dto.getVariants()) {
                if (vDto.getSku() == null || vDto.getSku().trim().isEmpty()) {
                    throw new ProductValidationException("Variant SKU is mandatory",
                            Map.of("sku", "Variant SKU is mandatory"));
                }
                if (variantRepository.existsBySku(vDto.getSku().trim())) {
                    throw new DuplicateResourceException("Variant SKU '" + vDto.getSku() + "' already exists");
                }
                ProductVariant variant = variantMapper.toEntity(vDto);
                variant.setProduct(product);
                product.getVariants().add(variant);
            }
        }

        Product saved = productRepository.save(product);

        // Auto-generate and publish 3D Try-On Asset for each variant of the created product
        autoGenerate3dAssetsForProduct(saved);

        auditService.logAudit(AuditAction.CREATE, "Product", saved.getId(), null,
                "Created product " + saved.getName() + " (REF: " + saved.getReference() + ")");

        return productMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(ProductSearchFilter filter, Pageable pageable) {
        return productRepository.findAll(ProductSpecification.filterBy(filter), pageable)
                .map(productMapper::toDto);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductRequestDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        String newRef = dto.getReference() != null && !dto.getReference().trim().isEmpty()
                ? dto.getReference().trim()
                : product.getReference();

        if (!newRef.equals(product.getReference()) && productRepository.existsByReference(newRef)) {
            throw new DuplicateResourceException("Product with reference '" + newRef + "' already exists");
        }

        String oldVal = "REF: " + product.getReference() + ", Name: " + product.getName();
        if (!newRef.equals(product.getReference())) {
            auditService.logAudit(AuditAction.REFERENCE_CHANGE, "Product", id, product.getReference(), newRef);
        }

        product.setReference(newRef);
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
            product.setActive(dto.getStatus() == ProductStatus.ACTIF);
        }
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getShortDescription() != null) product.setShortDescription(dto.getShortDescription());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getProductType() != null) product.setProductType(dto.getProductType());
        if (dto.getModel() != null) product.setModel(dto.getModel());
        if (dto.getGender() != null) product.setGender(dto.getGender());
        if (dto.getTargetAge() != null) product.setTargetAge(dto.getTargetAge());
        if (dto.getMaterial() != null) product.setMaterial(dto.getMaterial());
        if (dto.getCollection() != null) product.setCollection(dto.getCollection());
        if (dto.getSupplier() != null) product.setSupplier(dto.getSupplier());
        if (dto.getActive() != null) product.setActive(dto.getActive());
        if (dto.getFeatured() != null) product.setFeatured(dto.getFeatured());
        if (dto.getSearchable() != null) product.setSearchable(dto.getSearchable());
        if (dto.getFrameShape() != null) product.setFrameShape(dto.getFrameShape());
        if (dto.getTryOn3dAvailable() != null) product.setTryOn3dAvailable(dto.getTryOn3dAvailable());
        if (dto.getModel3dUrl() != null) product.setModel3dUrl(dto.getModel3dUrl());
        if (dto.getModel3dConfig() != null) product.setModel3dConfig(dto.getModel3dConfig());
        if (dto.getVirtualTryOnEnabled() != null) product.setVirtualTryOnEnabled(dto.getVirtualTryOnEnabled());
        if (dto.getImageUrl() != null) product.setImageUrl(dto.getImageUrl());

        // Dynamic 3D model synthesis if missing
        if (Boolean.TRUE.equals(product.getTryOn3dAvailable()) && (product.getModel3dConfig() == null || product.getModel3dConfig().isBlank())) {
            String shape = product.getFrameShape() != null ? product.getFrameShape().name() : "RECTANGLE";
            product.setModel3dConfig("{\"frameShape\":\"" + shape + "\",\"material\":\"" + (product.getMaterial() != null ? product.getMaterial() : "ACETATE") + "\",\"scale\":1.0}");
        }

        resolveBrandAndCategory(dto, product);

        Product saved = productRepository.save(product);

        // Auto-generate and publish 3D Try-On Asset for each variant of the updated product
        autoGenerate3dAssetsForProduct(saved);

        auditService.logAudit(AuditAction.UPDATE, "Product", saved.getId(), oldVal, "Updated product " + saved.getName());

        return productMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto patchProduct(Long id, Map<String, Object> updates) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (updates.containsKey("name")) {
            product.setName((String) updates.get("name"));
        }
        if (updates.containsKey("reference")) {
            String newRef = ((String) updates.get("reference")).trim();
            if (!newRef.equals(product.getReference()) && productRepository.existsByReference(newRef)) {
                throw new DuplicateResourceException("Product with reference '" + newRef + "' already exists");
            }
            product.setReference(newRef);
        }
        if (updates.containsKey("active")) {
            product.setActive((Boolean) updates.get("active"));
        }
        if (updates.containsKey("status") && updates.get("status") != null) {
            ProductStatus status = ProductStatus.valueOf(updates.get("status").toString());
            product.setStatus(status);
            product.setActive(status == ProductStatus.ACTIF);
        }
        if (updates.containsKey("model")) {
            product.setModel((String) updates.get("model"));
        }
        if (updates.containsKey("collection")) {
            product.setCollection((String) updates.get("collection"));
        }
        if (updates.containsKey("material")) {
            product.setMaterial((String) updates.get("material"));
        }
        if (updates.containsKey("description")) {
            product.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("frameShape") && updates.get("frameShape") != null) {
            product.setFrameShape(FrameShape.valueOf(updates.get("frameShape").toString()));
        }
        if (updates.containsKey("gender") && updates.get("gender") != null) {
            product.setGender(Gender.valueOf(updates.get("gender").toString()));
        }
        if (updates.containsKey("targetAge") && updates.get("targetAge") != null) {
            product.setTargetAge(TargetAge.valueOf(updates.get("targetAge").toString()));
        }
        if (updates.containsKey("featured")) {
            product.setFeatured((Boolean) updates.get("featured"));
        }
        if (updates.containsKey("searchable")) {
            product.setSearchable((Boolean) updates.get("searchable"));
        }

        Product saved = productRepository.save(product);
        auditService.logAudit(AuditAction.UPDATE, "Product", saved.getId(), null, "Patched product " + saved.getName());
        return productMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Soft Delete
        product.setActive(false);
        productRepository.save(product);

        auditService.logAudit(AuditAction.DELETE, "Product", id, "Name: " + product.getName(), "Soft deleted (active=false)");
    }

    @Override
    @Transactional
    public ProductResponseDto toggleProductActive(Long id, boolean active) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setActive(active);
        Product saved = productRepository.save(product);

        AuditAction action = active ? AuditAction.ACTIVATE : AuditAction.DEACTIVATE;
        auditService.logAudit(action, "Product", id, null, active ? "Activated" : "Deactivated");

        return productMapper.toDto(saved);
    }

    private void resolveBrandAndCategory(ProductRequestDto dto, Product product) {
        if (dto.getBrandId() != null) {
            Brand brand = brandRepository.findById(dto.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + dto.getBrandId()));
            product.setBrandEntity(brand);
        }

        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));
            product.setCategoryEntity(cat);
        }

        if (dto.getSubCategoryId() != null) {
            Category subCat = categoryRepository.findById(dto.getSubCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found with id: " + dto.getSubCategoryId()));
            product.setSubCategory(subCat);
        }
    }

    /**
     * Auto-generates and publishes a 3D Try-On Asset for each variant of a product.
     * Ensures that as soon as an admin enters a product with images, a 3D model is
     * immediately ready and published for client virtual try-on.
     */
    private void autoGenerate3dAssetsForProduct(Product product) {
        if (product == null || product.getVariants() == null || product.getVariants().isEmpty()) {
            return;
        }

        for (ProductVariant variant : product.getVariants()) {
            if (variant == null || variant.getId() == null) continue;
            // Check if published 3D asset already exists
            boolean hasPublished = assetRepository != null && assetRepository.findFirstByVariantIdAndStatus(variant.getId(), com.optician.backend.model.enums.TryOnAssetStatus.PUBLISHED).isPresent();
            if (!hasPublished) {
                String shape = product.getFrameShape() != null ? product.getFrameShape().name().toLowerCase() : "rectangle";
                String modelUrl = product.getModel3dUrl() != null && !product.getModel3dUrl().isBlank()
                        ? product.getModel3dUrl()
                        : "/uploads/models/procedural_" + shape + ".glb";

                com.optician.backend.model.VirtualTryOnAsset asset = com.optician.backend.model.VirtualTryOnAsset.builder()
                        .variant(variant)
                        .modelUrl(modelUrl)
                        .thumbnailUrl(product.getImageUrl())
                        .format("GLB")
                        .status(com.optician.backend.model.enums.TryOnAssetStatus.PUBLISHED)
                        .version(1)
                        .scale(1.0)
                        .positionX(0.0)
                        .positionY(0.0)
                        .positionZ(0.0)
                        .rotationX(0.0)
                        .rotationY(0.0)
                        .rotationZ(0.0)
                        .build();

                if (assetRepository != null) {
                    assetRepository.save(asset);
                }
                log.info(">>> 3D Asset généré et publié automatiquement pour le produit '{}' (Variant ID: {})", product.getName(), variant.getId());
            }
        }
    }
}
