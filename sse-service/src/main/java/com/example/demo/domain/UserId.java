package com.example.demo.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value Object representing a user identifier
 * Used for user-specific event targeting
 */
@Getter
@EqualsAndHashCode
public final class UserId {

    private final String value;

    private UserId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }
        this.value = value;
    }

    public static UserId from(String value) {
        return new UserId(value);
    }

    public static UserId anonymous() {
        return new UserId("anonymous");
    }

    public boolean isAnonymous() {
        return "anonymous".equals(value);
    }

    @Override
    public String toString() {
        return value;
    }
}