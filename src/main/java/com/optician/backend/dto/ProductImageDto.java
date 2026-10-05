package com.optician.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageDto {

    private Long id;
    private Long productId;
    private Long variantId;

    @NotBlank(message = "Image URL is mandatory")
    private String url;

    private String altText;
    private Integer displayOrder;
    private Boolean primaryImage;
    private LocalDateTime createdAt;
}
