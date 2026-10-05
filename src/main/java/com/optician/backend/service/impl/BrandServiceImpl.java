package com.optician.backend.service.impl;

import com.optician.backend.dto.BrandRequestDto;
import com.optician.backend.dto.BrandResponseDto;
import com.optician.backend.exception.DuplicateResourceException;
import com.optician.backend.exception.ProductInUseException;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.mapper.BrandMapper;
import com.optician.backend.model.Brand;
import com.optician.backend.repository.BrandRepository;
import com.optician.backend.repository.ProductRepository;
import com.optician.backend.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandMapper brandMapper;

    @Override
    @Transactional
    public BrandResponseDto createBrand(BrandRequestDto dto) {
        if (brandRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Brand already exists with name: " + dto.getName());
        }
        Brand brand = brandMapper.toEntity(dto);
        Brand saved = brandRepository.save(brand);
        return brandMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponseDto getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        return brandMapper.toDto(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponseDto> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(brandMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BrandResponseDto updateBrand(Long id, BrandRequestDto dto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        if (dto.getName() != null && !dto.getName().equalsIgnoreCase(brand.getName()) && brandRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Brand already exists with name: " + dto.getName());
        }

        brandMapper.updateEntityFromDto(dto, brand);
        Brand saved = brandRepository.save(brand);
        return brandMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        long productCount = productRepository.countByBrandEntityId(id);
        if (productCount > 0) {
            throw new ProductInUseException("Cannot delete brand '" + brand.getName() + "' because it is linked to " + productCount + " products");
        }

        brandRepository.delete(brand);
    }
}
