package com.example.demo.manager;

import com.example.demo.domain.SubscriptionId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Singleton SSE Connection Manager
 * Centralized management of all SSE emitter connections
 * Thread-safe singleton pattern using Spring's @Component
 */
@Slf4j
@Component
public class SseConnectionManager {

    // Thread-safe storage for all active emitter connections
    private final Map<SubscriptionId, SseEmitter> activeConnections = new ConcurrentHashMap<>();

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    /**
     * Create and store a new SSE connection
     *
     * @param subscriptionId Unique subscription identifier
     * @return Configured SseEmitter
     */
    public SseEmitter createConnection(SubscriptionId subscriptionId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Setup lifecycle handlers
        setupEmitterCallbacks(emitter, subscriptionId);

        // Store the connection
        activeConnections.put(subscriptionId, emitter);

        log.info("SSE connection created: {} (Total active: {})",
                subscriptionId, activeConnections.size());

        return emitter;
    }

    /**
     * Get an existing connection
     *
     * @param subscriptionId Subscription identifier
     * @return Optional containing the emitter if found
     */
    public Optional<SseEmitter> getConnection(SubscriptionId subscriptionId) {
        return Optional.ofNullable(activeConnections.get(subscriptionId));
    }

    /**
     * Remove and close a connection
     *
     * @param subscriptionId Subscription identifier
     */
    public void removeConnection(SubscriptionId subscriptionId) {
        SseEmitter emitter = activeConnections.remove(subscriptionId);

        if (emitter != null) {
            try {
                emitter.complete();
                log.info("SSE connection removed: {} (Total active: {})",
                        subscriptionId, activeConnections.size());
            } catch (Exception e) {
                log.debug("Error completing emitter for {}: {}", subscriptionId, e.getMessage());
            }
        }
    }

    /**
     * Send data to a specific connection
     *
     * @param subscriptionId Subscription identifier
     * @param eventBuilder SSE event builder
     * @return true if sent successfully, false otherwise
     */
    public boolean sendToConnection(SubscriptionId subscriptionId,
                                   SseEmitter.SseEventBuilder eventBuilder) {
        return getConnection(subscriptionId)
                .map(emitter -> {
                    try {
                        emitter.send(eventBuilder);
                        return true;
                    } catch (IOException e) {
                        log.error("Failed to send to connection {}: {}", subscriptionId, e.getMessage());
                        removeConnection(subscriptionId);
                        return false;
                    }
                })
                .orElse(false);
    }

    /**
     * Send data to a specific connection (raw data)
     *
     * @param subscriptionId Subscription identifier
     * @param data Data to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendToConnection(SubscriptionId subscriptionId, Object data) {
        return sendToConnection(subscriptionId, SseEmitter.event().data(data));
    }

    /**
     * Get all active connection IDs
     *
     * @return List of subscription IDs
     */
    public List<SubscriptionId> getAllConnectionIds() {
        return List.copyOf(activeConnections.keySet());
    }

    /**
     * Get all active emitters
     *
     * @return List of active SseEmitters
     */
    public List<SseEmitter> getAllConnections() {
        return List.copyOf(activeConnections.values());
    }

    /**
     * Get active connection count
     *
     * @return Number of active connections
     */
    public int getConnectionCount() {
        return activeConnections.size();
    }

    /**
     * Check if a connection exists
     *
     * @param subscriptionId Subscription identifier
     * @return true if connection exists
     */
    public boolean hasConnection(SubscriptionId subscriptionId) {
        return activeConnections.containsKey(subscriptionId);
    }

    /**
     * Remove all connections (cleanup)
     */
    public void removeAllConnections() {
        log.info("Removing all {} active SSE connections", activeConnections.size());

        activeConnections.keySet().forEach(this::removeConnection);
        activeConnections.clear();
    }

    /**
     * Get connections by filter predicate
     *
     * @param filter Filter predicate
     * @return Filtered map of connections
     */
    public Map<SubscriptionId, SseEmitter> getConnectionsBy(
            java.util.function.Predicate<Map.Entry<SubscriptionId, SseEmitter>> filter) {
        return activeConnections.entrySet().stream()
                .filter(filter)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Setup emitter lifecycle callbacks
     */
    private void setupEmitterCallbacks(SseEmitter emitter, SubscriptionId subscriptionId) {
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: {}", subscriptionId);
            activeConnections.remove(subscriptionId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout: {}", subscriptionId);
            removeConnection(subscriptionId);
        });

        emitter.onError(throwable -> {
            log.error("SSE connection error: {}", subscriptionId, throwable);
            removeConnection(subscriptionId);
        });
    }

    /**
     * Send welcome message to a new connection
     *
     * @param subscriptionId Subscription identifier
     * @param message Welcome message
     */
    public void sendWelcomeMessage(SubscriptionId subscriptionId, String message) {
        sendToConnection(subscriptionId,
                SseEmitter.event()
                        .id(subscriptionId.toString())
                        .name("connection")
                        .data(message));
    }

    /**
     * Get statistics about connections
     *
     * @return Map with connection statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "totalConnections", activeConnections.size(),
                "connectionIds", activeConnections.keySet().stream()
                        .map(SubscriptionId::toString)
                        .collect(Collectors.toList())
        );
    }
}