package org.esprit.security;

public enum Role {
    USER,
    ADMIN;

    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        return Role.valueOf(value.trim().toUpperCase());
    }
}
