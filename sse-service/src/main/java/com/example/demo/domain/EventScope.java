package com.example.demo.domain;

/**
 * Defines the scope of event distribution
 * GLOBAL: Broadcasted to all subscribers
 * USER_SPECIFIC: Targeted to specific user(s)
 */
public enum EventScope {
    GLOBAL,
    USER_SPECIFIC
}