package com.optician.backend.mapper;

import com.optician.backend.dto.CategoryRequestDto;
import com.optician.backend.dto.CategoryResponseDto;
import com.optician.backend.model.Category;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequestDto dto) {
        if (dto == null) return null;
        return Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .image(dto.getImage())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }

    public CategoryResponseDto toDto(Category entity) {
        if (entity == null) return null;
        List<CategoryResponseDto> subs = entity.getSubCategories() != null
                ? entity.getSubCategories().stream().map(this::toDto).toList()
                : Collections.emptyList();

        return CategoryResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .parentName(entity.getParent() != null ? entity.getParent().getName() : null)
                .subCategories(subs)
                .active(entity.getActive())
                .image(entity.getImage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDto(CategoryRequestDto dto, Category entity) {
        if (dto == null || entity == null) return;
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getImage() != null) entity.setImage(dto.getImage());
        if (dto.getActive() != null) entity.setActive(dto.getActive());
    }
}
