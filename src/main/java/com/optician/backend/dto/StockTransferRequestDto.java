package com.optician.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferRequestDto {

    @NotNull(message = "Magasin source obligatoire")
    private Long sourceStoreId;

    @NotNull(message = "Magasin cible obligatoire")
    private Long targetStoreId;

    @NotEmpty(message = "Au moins un article est requis")
    @Valid
    private List<StockTransferItemDto> items;

    private String notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockTransferItemDto {

        @NotNull(message = "ID variante obligatoire")
        private Long productVariantId;

        @NotNull(message = "Quantité obligatoire")
        private Integer quantityRequested;
    }
}
