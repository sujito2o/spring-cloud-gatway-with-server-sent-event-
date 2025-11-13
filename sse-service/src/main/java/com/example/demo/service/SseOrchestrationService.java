package com.example.demo.service;

import com.example.demo.domain.EventScope;
import com.example.demo.domain.SseEvent;
import com.example.demo.domain.UserId;
import com.example.demo.model.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

/**
 * Facade Service for SSE operations
 * Provides a simplified, unified interface to the SSE subsystem
 * Facade Pattern: Simplifies complex subsystem interactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseOrchestrationService {

    private final SubscriptionService subscriptionService;
    private final EventPublisher eventPublisher;

    /**
     * Subscribe a user to SSE events
     */
    public SseEmitter subscribe(String userIdStr, String clientInfo, Set<EventType> eventTypes) {
        UserId userId = (userIdStr != null && !userIdStr.isBlank())
                ? UserId.from(userIdStr)
                : UserId.anonymous();

        return subscriptionService.subscribe(userId, clientInfo, eventTypes);
    }

    /**
     * Broadcast global event to all subscribers
     */
    public void broadcastGlobalEvent(EventType type, String title, String message, String severity) {
        SseEvent event = SseEvent.createGlobal(type, title, message, severity);
        eventPublisher.publish(event);
        log.info("Broadcasted global event: {} - {}", type, title);
    }

    /**
     * Send event to specific user
     */
    public void sendUserEvent(String userIdStr, EventType type, String title,
                              String message, String severity) {
        UserId userId = UserId.from(userIdStr);
        SseEvent event = SseEvent.createForUser(userId, type, title, message, severity);
        eventPublisher.publish(event);
        log.info("Sent user-specific event to {}: {} - {}", userId, type, title);
    }

    /**
     * Get active connection count
     */
    public long getActiveConnectionCount() {
        return subscriptionService.getActiveSubscriptionCount();
    }

    /**
     * Get subscription count for a specific user
     */
    public int getUserSubscriptionCount(String userIdStr) {
        if (userIdStr == null || userIdStr.isBlank()) {
            return 0;
        }
        return subscriptionService.getUserSubscriptionCount(UserId.from(userIdStr));
    }
}