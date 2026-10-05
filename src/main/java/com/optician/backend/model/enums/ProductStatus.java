package com.optician.backend.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProductStatus {
    ACTIF,
    INACTIF,
    BROUILLON;

    @JsonCreator
    public static ProductStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) return ACTIF;
        String val = value.trim().toUpperCase();
        return switch (val) {
            case "ACTIVE", "ACTIF" -> ACTIF;
            case "INACTIVE", "INACTIF" -> INACTIF;
            case "DRAFT", "BROUILLON" -> BROUILLON;
            default -> ACTIF;
        };
    }
}

