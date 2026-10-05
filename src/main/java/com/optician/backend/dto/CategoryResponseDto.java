package com.optician.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDto {

    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private List<CategoryResponseDto> subCategories;
    private Boolean active;
    private String image;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
