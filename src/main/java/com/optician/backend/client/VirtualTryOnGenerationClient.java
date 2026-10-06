package com.optician.backend.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class VirtualTryOnGenerationClient {

    private final RestTemplate restTemplate;
    private final String serviceUrl;

    public VirtualTryOnGenerationClient(@Value("${optivision.3d-service.url:http://localhost:8001/api/v1}") String serviceUrl) {
        this.restTemplate = new RestTemplate();
        this.serviceUrl = serviceUrl;
    }

    @Data
    public static class PythonJobResponse {
        private String jobId;
        private Long variantId;
        private String status;
        private Integer progress;
        private String statusDetails;
        private String modelUrl;
        private Integer version;
        private PythonQualityScore scores;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    public static class PythonQualityScore {
        private Integer overall;
        private Integer geometry;
        private Integer symmetry;
        private Integer scale;
        private Integer material;
    }

    public boolean isServiceHealthy() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(serviceUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Microservice 3D ({}): non joignable: {}", serviceUrl, e.getMessage());
            return false;
        }
    }

    public PythonJobResponse triggerGeneration(Long variantId, String shape, Integer lensWidth, Integer bridgeWidth, Integer templeLength, List<MultipartFile> files) {
        String endpoint = serviceUrl + "/generations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("variantId", variantId.toString());
        body.add("shape", shape != null ? shape : "RECTANGULAIRE");
        body.add("lensWidthMm", String.valueOf(lensWidth != null ? lensWidth : 52));
        body.add("bridgeWidthMm", String.valueOf(bridgeWidth != null ? bridgeWidth : 18));
        body.add("templeLengthMm", String.valueOf(templeLength != null ? templeLength : 140));

        if (files != null) {
            for (MultipartFile file : files) {
                try {
                    ByteArrayResource contentsAsResource = new ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
                        }
                    };
                    body.add("files", contentsAsResource);
                } catch (IOException e) {
                    log.error("Erreur lecture fichier pour service 3D: {}", e.getMessage());
                }
            }
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<PythonJobResponse> response = restTemplate.exchange(endpoint, HttpMethod.POST, requestEntity, PythonJobResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Erreur lors de l'appel au service Python 3D: {}", e.getMessage());
            throw new RuntimeException("Erreur de communication avec le microservice Python 3D: " + e.getMessage());
        }
    }

    public PythonJobResponse getJobStatus(String jobId) {
        String endpoint = serviceUrl + "/generations/" + jobId;
        try {
            ResponseEntity<PythonJobResponse> response = restTemplate.getForEntity(endpoint, PythonJobResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Erreur récupération statut job 3D ({}): {}", jobId, e.getMessage());
            return null;
        }
    }
}
