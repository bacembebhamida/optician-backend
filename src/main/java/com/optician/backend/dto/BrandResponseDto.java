package com.optician.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandResponseDto {

    private Long id;
    private String name;
    private String description;
    private String logo;
    private String website;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
