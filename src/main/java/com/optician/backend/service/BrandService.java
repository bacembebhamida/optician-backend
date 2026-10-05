package com.optician.backend.service;

import com.optician.backend.dto.BrandRequestDto;
import com.optician.backend.dto.BrandResponseDto;

import java.util.List;

public interface BrandService {
    BrandResponseDto createBrand(BrandRequestDto dto);
    BrandResponseDto getBrandById(Long id);
    List<BrandResponseDto> getAllBrands();
    BrandResponseDto updateBrand(Long id, BrandRequestDto dto);
    void deleteBrand(Long id);
}
