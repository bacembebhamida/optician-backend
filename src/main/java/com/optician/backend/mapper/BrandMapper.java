package com.optician.backend.mapper;

import com.optician.backend.dto.BrandRequestDto;
import com.optician.backend.dto.BrandResponseDto;
import com.optician.backend.model.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public Brand toEntity(BrandRequestDto dto) {
        if (dto == null) return null;
        return Brand.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .logo(dto.getLogo())
                .website(dto.getWebsite())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }

    public BrandResponseDto toDto(Brand entity) {
        if (entity == null) return null;
        return BrandResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .logo(entity.getLogo())
                .website(entity.getWebsite())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDto(BrandRequestDto dto, Brand entity) {
        if (dto == null || entity == null) return;
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getLogo() != null) entity.setLogo(dto.getLogo());
        if (dto.getWebsite() != null) entity.setWebsite(dto.getWebsite());
        if (dto.getActive() != null) entity.setActive(dto.getActive());
    }
}
