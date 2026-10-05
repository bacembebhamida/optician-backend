package com.optician.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandRequestDto {

    @NotBlank(message = "Brand name is mandatory")
    private String name;

    private String description;
    private String logo;
    private String website;

    @Builder.Default
    private Boolean active = true;
}
