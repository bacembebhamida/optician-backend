package com.optician.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequestDto {

    @NotBlank(message = "Category name is mandatory")
    private String name;

    private String description;
    private Long parentId;
    private String image;

    @Builder.Default
    private Boolean active = true;
}
