package com.example.demo.model;

/**
 * Enum representing different types of server-sent events
 */
public enum EventType {
    NOTIFICATION,
    SYSTEM_UPDATE,
    USER_ACTION,
    METRICS,
    ALERT
}