package com.optician.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationRequest {
    private String faceShape;       // OVAL, CARRE, ROND, COEUR
    private String preferredStyle;   // VINTAGE, SPORT, ELEGANT, MINIMALIST
    private Double pupillaryDistance; // e.g. 63.5
    private String gender;           // HOMME, FEMME, MIXTE
}
