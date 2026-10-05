package com.optician.backend.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FrameShape {
    OVALE,
    CARRE,
    ROND,
    PANTOS,
    PAPILLON,
    RECTANGULAIRE,
    AVIATEUR,
    OCTOGONAL,
    AUTRE;

    @JsonCreator
    public static FrameShape fromString(String value) {
        if (value == null || value.trim().isEmpty()) return CARRE;
        String val = value.trim().toUpperCase();
        return switch (val) {
            case "RECTANGLE", "RECTANGULAIRE" -> RECTANGULAIRE;
            case "OCTOGONALE", "OCTOGONAL" -> OCTOGONAL;
            case "PANTO", "PANTOS" -> PANTOS;
            default -> {
                for (FrameShape fs : FrameShape.values()) {
                    if (fs.name().equalsIgnoreCase(val)) yield fs;
                }
                yield AUTRE;
            }
        };
    }
}

