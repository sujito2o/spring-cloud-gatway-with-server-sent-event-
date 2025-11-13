package com.example.demo.service;

import com.example.demo.domain.Subscription;
import com.example.demo.domain.SubscriptionId;
import com.example.demo.domain.UserId;
import com.example.demo.manager.SseConnectionManager;
import com.example.demo.model.EventType;
import com.example.demo.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.InetAddress;
import java.util.Set;

/**
 * Service for managing SSE subscriptions
 * Single Responsibility: Focus only on subscription lifecycle
 * Uses singleton SseConnectionManager for connection management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SseConnectionManager connectionManager;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Create a new subscription
     * Uses singleton SseConnectionManager for connection management
     */
    public SseEmitter subscribe(UserId userId, String clientInfo, Set<EventType> eventTypes) {
        String serverId = getServerId();

        // Create subscription domain object
        Subscription subscription = Subscription.create(userId, clientInfo, eventTypes, serverId);
        SubscriptionId subscriptionId = subscription.getSubscriptionId();

        // Create and store SSE connection using singleton manager
        SseEmitter emitter = connectionManager.createConnection(subscriptionId);

        // Store subscription metadata
        subscriptionRepository.save(subscription);

        log.info("Created subscription: {} for user: {} on server: {}",
                subscriptionId, userId, serverId);

        // Send welcome message using connection manager
        connectionManager.sendWelcomeMessage(subscriptionId,
                "{\"message\":\"Connected to SSE stream\",\"subscriptionId\":\"" + subscriptionId + "\"}");

        return emitter;
    }

    /**
     * Remove a subscription
     * Removes both metadata and connection via singleton manager
     */
    public void unsubscribe(SubscriptionId subscriptionId) {
        subscriptionRepository.remove(subscriptionId);
        connectionManager.removeConnection(subscriptionId);
        log.info("Removed subscription: {}", subscriptionId);
    }

    /**
     * Get active subscription count
     */
    public long getActiveSubscriptionCount() {
        return subscriptionRepository.count();
    }

    /**
     * Get subscriptions for a specific user
     */
    public int getUserSubscriptionCount(UserId userId) {
        return subscriptionRepository.findByUserId(userId).size();
    }

    /**
     * Heartbeat mechanism - update subscription timestamps
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void heartbeat() {
        subscriptionRepository.findAll().forEach(subscription -> {
            subscription.updateHeartbeat();
            subscriptionRepository.save(subscription);
        });
        log.debug("Heartbeat completed for {} subscriptions", subscriptionRepository.count());
    }

    /**
     * Cleanup expired subscriptions
     */
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    public void cleanupExpired() {
        subscriptionRepository.removeExpired();
    }

    private String getServerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + ":" + serverPort;
        } catch (Exception e) {
            return "unknown-server:" + serverPort;
        }
    }
}