package com.optician.backend.dto;

import com.optician.backend.model.enums.StockMovementType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementSearchFilter {

    private Long productVariantId;
    private Long storeId;
    private StockMovementType type;
    private String reference;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
