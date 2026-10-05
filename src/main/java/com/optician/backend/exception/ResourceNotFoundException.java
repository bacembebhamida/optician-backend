package com.optician.backend.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object fieldValue) {
        super(String.format("%s non trouvé avec l'identifiant : %s", resourceName, fieldValue));
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s non trouvé avec %s : %s", resourceName, fieldName, fieldValue));
    }
}
