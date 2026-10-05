package com.optician.backend.service;

import com.optician.backend.dto.VirtualTryOnAssetRequestDto;
import com.optician.backend.dto.VirtualTryOnAssetResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VirtualTryOnAssetService {

    List<VirtualTryOnAssetResponseDto> getAssetsByVariantId(Long variantId);

    VirtualTryOnAssetResponseDto getPublishedAssetByVariantId(Long variantId);

    VirtualTryOnAssetResponseDto uploadGlbModel(Long variantId, MultipartFile file);

    VirtualTryOnAssetResponseDto generateFromImages(Long variantId, List<MultipartFile> images);

    VirtualTryOnAssetResponseDto updateCalibration(Long assetId, VirtualTryOnAssetRequestDto dto);

    VirtualTryOnAssetResponseDto validateAsset(Long assetId, String validatorName);

    VirtualTryOnAssetResponseDto publishAsset(Long assetId);

    void deleteAsset(Long assetId);
}
