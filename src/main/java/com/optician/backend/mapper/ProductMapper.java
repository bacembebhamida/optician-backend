package com.optician.backend.mapper;

import com.optician.backend.dto.ProductImageDto;
import com.optician.backend.dto.ProductRequestDto;
import com.optician.backend.dto.ProductResponseDto;
import com.optician.backend.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class ProductMapper {

    private final ProductVariantMapper variantMapper;

    public Product toEntity(ProductRequestDto dto) {
        if (dto == null) return null;
        return Product.builder()
                .reference(dto.getReference())
                .name(dto.getName())
                .shortDescription(dto.getShortDescription())
                .description(dto.getDescription())
                .productType(dto.getProductType())
                .model(dto.getModel())
                .gender(dto.getGender())
                .targetAge(dto.getTargetAge())
                .material(dto.getMaterial())
                .collection(dto.getCollection())
                .supplier(dto.getSupplier())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .status(dto.getStatus() != null ? dto.getStatus()
                        : (dto.getActive() != null && !dto.getActive()
                        ? com.optician.backend.model.enums.ProductStatus.INACTIF
                        : com.optician.backend.model.enums.ProductStatus.ACTIF))
                .featured(dto.getFeatured() != null ? dto.getFeatured() : false)
                .searchable(dto.getSearchable() != null ? dto.getSearchable() : true)
                .frameShape(dto.getFrameShape())
                .tryOn3dAvailable(dto.getTryOn3dAvailable() != null ? dto.getTryOn3dAvailable() : true)
                .model3dUrl(dto.getModel3dUrl())
                .model3dConfig(dto.getModel3dConfig())
                .virtualTryOnEnabled(dto.getVirtualTryOnEnabled() != null ? dto.getVirtualTryOnEnabled() : true)
                .imageUrl(dto.getImageUrl())
                .build();
    }

    public ProductResponseDto toDto(Product entity) {
        if (entity == null) return null;

        List<ProductImageDto> imageDtos = entity.getImages() != null
                ? entity.getImages().stream().map(variantMapper::toImageDto).toList()
                : Collections.emptyList();

        return ProductResponseDto.builder()
                .id(entity.getId())
                .reference(entity.getReference())
                .name(entity.getName())
                .shortDescription(entity.getShortDescription())
                .description(entity.getDescription())
                .productType(entity.getProductType())
                .categoryId(entity.getCategoryEntity() != null ? entity.getCategoryEntity().getId() : null)
                .categoryName(entity.getCategory())
                .category(entity.getCategory())
                .subCategoryId(entity.getSubCategory() != null ? entity.getSubCategory().getId() : null)
                .subCategoryName(entity.getSubCategory() != null ? entity.getSubCategory().getName() : null)
                .brandId(entity.getBrandEntity() != null ? entity.getBrandEntity().getId() : null)
                .brandName(entity.getBrand())
                .brand(entity.getBrand())
                .model(entity.getModel())
                .gender(entity.getGender())
                .targetAge(entity.getTargetAge())
                .material(entity.getMaterial())
                .collection(entity.getCollection())
                .supplier(entity.getSupplier())
                .frameShape(entity.getFrameShape())
                .active(entity.getActive())
                .status(entity.getStatus() != null ? entity.getStatus()
                        : (Boolean.TRUE.equals(entity.getActive())
                        ? com.optician.backend.model.enums.ProductStatus.ACTIF
                        : com.optician.backend.model.enums.ProductStatus.INACTIF))
                .featured(entity.getFeatured())
                .searchable(entity.getSearchable())
                .tryOn3dAvailable(entity.getTryOn3dAvailable())
                .model3dUrl(entity.getModel3dUrl())
                .model3dConfig(entity.getModel3dConfig())
                .virtualTryOnEnabled(entity.getVirtualTryOnEnabled())
                .imageUrl(entity.getImageUrl())
                .price(entity.getPrice())
                .stock(entity.getStock())
                .variantCount(entity.getVariantCount())
                .variants(entity.getVariants() != null
                        ? entity.getVariants().stream().map(variantMapper::toDto).toList()
                        : Collections.emptyList())
                .images(imageDtos)
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
