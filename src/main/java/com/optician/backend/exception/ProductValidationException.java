package com.optician.backend.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ProductValidationException extends RuntimeException {

    private final Map<String, String> fields;

    public ProductValidationException(String message) {
        super(message);
        this.fields = Map.of();
    }

    public ProductValidationException(String message, Map<String, String> fields) {
        super(message);
        this.fields = fields;
    }
}
