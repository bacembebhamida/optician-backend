package com.optician.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementResponse {
    private Double totalPrice;
    private Double secuCoverage;
    private Double mutuelleCoverage;
    private Double totalCoverage;
    private Double resteAChargeClient;
    private Boolean isFullyCovered;
    private String offerLabel;
}
