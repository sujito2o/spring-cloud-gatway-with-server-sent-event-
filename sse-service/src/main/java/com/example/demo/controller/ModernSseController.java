package com.example.demo.controller;

import com.example.demo.model.EventType;
import com.example.demo.model.ServerEvent;
import com.example.demo.service.SseOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modern SSE Controller using clean architecture
 * Demonstrates SOLID principles and clean code practices
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/events")
@RequiredArgsConstructor
public class ModernSseController {

    private final SseOrchestrationService orchestrationService;

    /**
     * Subscribe to SSE stream with optional user ID
     * Supports both authenticated and anonymous users
     *
     * @param userId Optional user identifier for user-specific events
     * @param clientInfo Optional client information
     * @param types Optional event types filter
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String clientInfo,
            @RequestParam(required = false) String[] types) {

        log.info("New SSE connection request - userId: {}, client: {}", userId, clientInfo);

        Set<EventType> eventTypes = parseEventTypes(types);

        return orchestrationService.subscribe(userId, clientInfo, eventTypes);
    }

    /**
     * Broadcast global event to all subscribers
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, String>> broadcastEvent(@RequestBody ServerEvent event) {
        log.info("Broadcasting global event: {}", event.getType());

        orchestrationService.broadcastGlobalEvent(
                event.getType(),
                event.getTitle(),
                event.getMessage(),
                event.getSeverity()
        );

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Event broadcasted to all subscribers",
                "eventType", event.getType().toString()
        ));
    }

    /**
     * Send event to specific user
     * NEW: User-targeted event delivery
     */
    @PostMapping("/send/{userId}")
    public ResponseEntity<Map<String, String>> sendUserEvent(
            @PathVariable String userId,
            @RequestBody ServerEvent event) {

        log.info("Sending user-specific event to {}: {}", userId, event.getType());

        orchestrationService.sendUserEvent(
                userId,
                event.getType(),
                event.getTitle(),
                event.getMessage(),
                event.getSeverity()
        );

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Event sent to user: " + userId,
                "eventType", event.getType().toString()
        ));
    }

    /**
     * Get connection statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long activeConnections = orchestrationService.getActiveConnectionCount();

        return ResponseEntity.ok(Map.of(
                "activeConnections", activeConnections,
                "timestamp", java.time.LocalDateTime.now()
        ));
    }

    /**
     * Get user-specific connection count
     * NEW: Check how many active connections a user has
     */
    @GetMapping("/stats/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String userId) {
        int userConnections = orchestrationService.getUserSubscriptionCount(userId);

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "activeConnections", userConnections,
                "timestamp", java.time.LocalDateTime.now()
        ));
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "activeConnections", orchestrationService.getActiveConnectionCount(),
                "timestamp", java.time.LocalDateTime.now()
        ));
    }

    private Set<EventType> parseEventTypes(String[] types) {
        if (types == null || types.length == 0) {
            return Set.of(); // Empty set means subscribe to all types
        }

        return Arrays.stream(types)
                .map(String::toUpperCase)
                .map(EventType::valueOf)
                .collect(Collectors.toSet());
    }
}