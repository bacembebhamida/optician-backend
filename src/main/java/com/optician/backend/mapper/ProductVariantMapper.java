package com.optician.backend.mapper;

import com.optician.backend.dto.ProductImageDto;
import com.optician.backend.dto.ProductVariantRequestDto;
import com.optician.backend.dto.ProductVariantResponseDto;
import com.optician.backend.model.ProductImage;
import com.optician.backend.model.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@SuppressWarnings("deprecation")
public class ProductVariantMapper {

    public ProductVariant toEntity(ProductVariantRequestDto dto) {
        if (dto == null) return null;
        return ProductVariant.builder()
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .color(dto.getColor())
                .size(dto.getSize())
                .purchasePrice(dto.getPurchasePrice())
                .sellingPrice(dto.getSellingPrice())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .stock(dto.getStock() != null ? dto.getStock() : 0)
                .build();
    }

    public ProductVariantResponseDto toDto(ProductVariant entity) {
        if (entity == null) return null;
        List<ProductImageDto> imageDtos = entity.getImages() != null
                ? entity.getImages().stream().map(this::toImageDto).toList()
                : Collections.emptyList();

        return ProductVariantResponseDto.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .sku(entity.getSku())
                .barcode(entity.getBarcode())
                .color(entity.getColor())
                .size(entity.getSize())
                .purchasePrice(entity.getPurchasePrice())
                .sellingPrice(entity.getSellingPrice())
                .active(entity.getActive())
                .stock(entity.getStock())
                .images(imageDtos)
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ProductImageDto toImageDto(ProductImage entity) {
        if (entity == null) return null;
        return ProductImageDto.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .variantId(entity.getVariant() != null ? entity.getVariant().getId() : null)
                .url(entity.getUrl())
                .altText(entity.getAltText())
                .displayOrder(entity.getDisplayOrder())
                .primaryImage(entity.getPrimaryImage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
