package com.optician.backend.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Gender {
    HOMME,
    FEMME,
    MIXTE,
    ENFANT;

    @JsonCreator
    public static Gender fromString(String value) {
        if (value == null || value.trim().isEmpty()) return MIXTE;
        String val = value.trim().toUpperCase();
        return switch (val) {
            case "UNISEX", "UNISEXE", "MIXTE" -> MIXTE;
            case "HOMME", "MAN", "MALE" -> HOMME;
            case "FEMME", "WOMAN", "FEMALE" -> FEMME;
            case "ENFANT", "KIDS", "CHILD" -> ENFANT;
            default -> {
                for (Gender g : Gender.values()) {
                    if (g.name().equalsIgnoreCase(val)) yield g;
                }
                yield MIXTE;
            }
        };
    }
}

