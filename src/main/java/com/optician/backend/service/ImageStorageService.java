package com.optician.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String storeImage(MultipartFile file, String folder);
    void deleteImage(String imageUrl);
}
