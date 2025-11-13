package com.example.demo.service;

import com.example.demo.domain.SseEvent;
import com.example.demo.strategy.EventPublishingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Context class for event publishing strategies
 * Delegates to appropriate strategy based on event scope
 * Open/Closed Principle: New strategies can be added without modifying this class
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final List<EventPublishingStrategy> strategies;

    /**
     * Publish event using the appropriate strategy
     * Strategy pattern: Delegates to the correct strategy at runtime
     */
    @Async
    public void publish(SseEvent event) {
        EventPublishingStrategy strategy = strategies.stream()
                .filter(s -> s.canHandle(event))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No publishing strategy found for event scope: " + event.getScope()));

        log.debug("Using strategy: {} for event: {}",
                strategy.getClass().getSimpleName(), event.getId());

        strategy.publish(event);
    }
}