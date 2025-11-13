package com.example.demo.strategy;

import com.example.demo.domain.SseEvent;

/**
 * Strategy interface for event publishing
 * Open/Closed Principle: Open for extension, closed for modification
 */
public interface EventPublishingStrategy {

    /**
     * Publish an event according to the strategy
     */
    void publish(SseEvent event);

    /**
     * Check if this strategy can handle the given event
     */
    boolean canHandle(SseEvent event);
}