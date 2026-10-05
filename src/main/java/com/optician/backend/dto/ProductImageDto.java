package com.optician.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonAlias({"sortOrder", "displayOrder"})
    private Integer displayOrder;

    @JsonAlias({"isPrimary", "primaryImage"})
    private Boolean primaryImage;

    private LocalDateTime createdAt;

    @JsonProperty("id")
    public void setId(Object rawId) {
        if (rawId == null) {
            this.id = null;
        } else if (rawId instanceof Number n) {
            this.id = n.longValue();
        } else {
            String str = String.valueOf(rawId);
            try {
                this.id = Long.parseLong(str);
            } catch (NumberFormatException e) {
                this.id = null;
            }
        }
    }
}

