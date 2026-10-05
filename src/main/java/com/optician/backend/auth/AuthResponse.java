package com.optician.backend.auth;

import com.optician.backend.model.UserRole;

import java.io.Serializable;

public record AuthResponse(Long id, String fullName, String email, UserRole role) implements Serializable {
}
