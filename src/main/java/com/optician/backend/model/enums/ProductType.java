package com.optician.backend.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProductType {
    OPTICAL_FRAME,
    SUNGLASSES,
    KIDS_GLASSES,
    CONTACT_LENSES,
    ACCESSORIES,
    OPTICAL_LENSES,
    CASES,
    CARE_PRODUCTS,
    OTHER;

    @JsonCreator
    public static ProductType fromString(String value) {
        if (value == null || value.trim().isEmpty()) return OPTICAL_FRAME;
        String val = value.trim().toUpperCase();
        return switch (val) {
            case "MONTURE", "OPTICAL_FRAME", "FRAME" -> OPTICAL_FRAME;
            case "SUNGLASSES", "LUNETTES_SOLEIL" -> SUNGLASSES;
            case "VERRE", "OPTICAL_LENSES" -> OPTICAL_LENSES;
            case "LENTILLE_CONTACT", "CONTACT_LENSES" -> CONTACT_LENSES;
            case "PRODUIT_ENTRETIEN", "CARE_PRODUCTS" -> CARE_PRODUCTS;
            case "ACCESSOIRE", "ACCESSOIRES", "ACCESSORIES" -> ACCESSORIES;
            case "CASES" -> CASES;
            case "KIDS_GLASSES", "LUNETTES_ENFANT" -> KIDS_GLASSES;
            default -> {
                for (ProductType pt : ProductType.values()) {
                    if (pt.name().equalsIgnoreCase(val)) yield pt;
                }
                yield OPTICAL_FRAME;
            }
        };
    }
}

