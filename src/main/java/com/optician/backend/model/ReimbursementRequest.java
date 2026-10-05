package com.optician.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementRequest {
    private Double framePrice;
    private Double lensPrice;
    private String offerCategory; // PANIER_100_SANTE or PANIER_LIBRE
    private String mutuelleLevel; // TIER_1_BASIC, TIER_2_CONFORT, TIER_3_PREMIUM
}
