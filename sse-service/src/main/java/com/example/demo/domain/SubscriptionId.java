package com.example.demo.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Value Object representing a unique subscription identifier
 * Immutable and self-validating
 */
@Getter
@EqualsAndHashCode
public final class SubscriptionId {

    private final String value;

    private SubscriptionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SubscriptionId cannot be null or empty");
        }
        this.value = value;
    }

    public static SubscriptionId generate() {
        return new SubscriptionId(UUID.randomUUID().toString());
    }

    public static SubscriptionId from(String value) {
        return new SubscriptionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}