package com.optician.backend.service.impl;

import com.optician.backend.dto.CategoryRequestDto;
import com.optician.backend.dto.CategoryResponseDto;
import com.optician.backend.exception.DuplicateResourceException;
import com.optician.backend.exception.ProductInUseException;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.mapper.CategoryMapper;
import com.optician.backend.model.Category;
import com.optician.backend.repository.CategoryRepository;
import com.optician.backend.repository.ProductRepository;
import com.optician.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Category already exists with name: " + dto.getName());
        }

        Category category = categoryMapper.toEntity(dto);

        if (dto.getParentId() != null) {
            Category parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + dto.getParentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return categoryMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findByParentIsNull().stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (dto.getName() != null && !dto.getName().equalsIgnoreCase(category.getName()) && categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Category already exists with name: " + dto.getName());
        }

        categoryMapper.updateEntityFromDto(dto, category);

        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + dto.getParentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return categoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        long productCount = productRepository.countByCategoryEntityId(id);
        if (productCount > 0) {
            throw new ProductInUseException("Cannot delete category '" + category.getName() + "' because it is linked to " + productCount + " products");
        }

        categoryRepository.delete(category);
    }
}
