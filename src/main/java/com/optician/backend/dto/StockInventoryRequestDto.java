package com.optician.backend.dto;

import com.optician.backend.model.enums.InventoryType;
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
public class StockInventoryRequestDto {

    @NotNull(message = "Type d'inventaire obligatoire")
    private InventoryType type;

    @NotNull(message = "ID du magasin obligatoire")
    private Long storeId;

    @NotEmpty(message = "Au moins un article est requis")
    @Valid
    private List<InventoryItemDto> items;

    private String notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryItemDto {

        @NotNull(message = "ID variante obligatoire")
        private Long productVariantId;

        @NotNull(message = "Quantité physique comptée obligatoire")
        private Integer physicalQuantity;

        private String notes;
    }
}
