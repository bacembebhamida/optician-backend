package com.optician.backend.service.impl;

import com.optician.backend.service.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class MinioObjectStorageServiceImpl implements ImageStorageService {

    @Value("${app.storage.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.storage.bucket:optivision-products}")
    private String bucket;

    @Override
    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        String filename = folder + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        log.info("Simulating upload of {} to MinIO/S3 bucket [{}]", filename, bucket);
        return endpoint + "/" + bucket + "/" + filename;
    }

    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl != null) {
            log.info("Simulating deletion of image [{}] from MinIO/S3 bucket [{}]", imageUrl, bucket);
        }
    }
}
