package com.example.demo.domain;

import com.example.demo.model.EventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Aggregate Root representing an SSE subscription
 * Encapsulates business rules and invariants
 */
@Getter
@Builder
public class Subscription {

    private final SubscriptionId subscriptionId;
    private final UserId userId;
    private final String clientInfo;
    private final Set<EventType> subscribedEventTypes;
    private final LocalDateTime createdAt;
    private LocalDateTime lastHeartbeat;
    private final String serverId;

    /**
     * Check if this subscription should receive an event
     */
    public boolean shouldReceive(EventType eventType, EventScope scope, UserId targetUserId) {
        // Check event type filter
        if (!subscribedEventTypes.isEmpty() && !subscribedEventTypes.contains(eventType)) {
            return false;
        }

        // Global events are received by all
        if (scope == EventScope.GLOBAL) {
            return true;
        }

        // User-specific events only for matching users
        return this.userId.equals(targetUserId);
    }

    /**
     * Update heartbeat timestamp
     */
    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
    }

    /**
     * Check if subscription is expired (no heartbeat for 2 minutes)
     */
    public boolean isExpired() {
        return lastHeartbeat.plusMinutes(2).isBefore(LocalDateTime.now());
    }

    /**
     * Get immutable set of subscribed event types
     */
    public Set<EventType> getSubscribedEventTypes() {
        return Collections.unmodifiableSet(subscribedEventTypes);
    }

    /**
     * Factory method for creating a subscription
     */
    public static Subscription create(UserId userId, String clientInfo,
                                     Set<EventType> eventTypes, String serverId) {
        return Subscription.builder()
                .subscriptionId(SubscriptionId.generate())
                .userId(userId)
                .clientInfo(Optional.ofNullable(clientInfo).orElse("Unknown"))
                .subscribedEventTypes(eventTypes != null ? eventTypes : Collections.emptySet())
                .createdAt(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .serverId(serverId)
                .build();
    }
}