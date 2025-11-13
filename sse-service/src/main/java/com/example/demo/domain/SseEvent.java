package com.example.demo.domain;

import com.example.demo.model.EventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain event representing an SSE event with scope awareness
 * Supports both global and user-specific event distribution
 */
@Getter
@Builder
public class SseEvent {

    private final String id;
    private final EventType type;
    private final String title;
    private final String message;
    private final LocalDateTime timestamp;
    private final String severity;
    private final Map<String, Object> metadata;

    // Event targeting
    private final EventScope scope;
    private final UserId targetUserId; // null for global events

    /**
     * Check if this event targets a specific user
     */
    public boolean isUserSpecific() {
        return scope == EventScope.USER_SPECIFIC && targetUserId != null;
    }

    /**
     * Get target user ID if event is user-specific
     */
    public Optional<UserId> getTargetUserId() {
        return Optional.ofNullable(targetUserId);
    }

    /**
     * Factory method for global events
     */
    public static SseEvent createGlobal(EventType type, String title, String message, String severity) {
        return SseEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .title(title)
                .message(message)
                .timestamp(LocalDateTime.now())
                .severity(severity)
                .scope(EventScope.GLOBAL)
                .build();
    }

    /**
     * Factory method for user-specific events
     */
    public static SseEvent createForUser(UserId userId, EventType type, String title,
                                         String message, String severity) {
        return SseEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .title(title)
                .message(message)
                .timestamp(LocalDateTime.now())
                .severity(severity)
                .scope(EventScope.USER_SPECIFIC)
                .targetUserId(userId)
                .build();
    }
}