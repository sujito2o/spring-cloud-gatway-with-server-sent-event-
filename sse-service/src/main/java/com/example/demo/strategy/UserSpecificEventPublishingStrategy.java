package com.example.demo.strategy;

import com.example.demo.domain.*;
import com.example.demo.manager.SseConnectionManager;
import com.example.demo.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for publishing user-specific events
 * Single Responsibility: Handles only targeted event distribution
 * Uses singleton SseConnectionManager for connection management
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSpecificEventPublishingStrategy implements EventPublishingStrategy {

    private final SubscriptionRepository subscriptionRepository;
    private final SseConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(SseEvent event) {
        UserId targetUserId = event.getTargetUserId()
                .orElseThrow(() -> new IllegalArgumentException("User-specific event must have targetUserId"));

        log.debug("Publishing user-specific event: {} to user: {}", event.getType(), targetUserId);

        List<Subscription> userSubscriptions = subscriptionRepository.findByUserId(targetUserId);

        if (userSubscriptions.isEmpty()) {
            log.warn("No active subscriptions found for user: {}", targetUserId);
            return;
        }

        List<SubscriptionId> failedSubscriptions = new ArrayList<>();
        int successCount = 0;

        for (Subscription subscription : userSubscriptions) {
            if (subscription.shouldReceive(event.getType(), event.getScope(), targetUserId)) {
                boolean sent = sendToSubscription(subscription.getSubscriptionId(), event);
                if (sent) {
                    successCount++;
                } else {
                    failedSubscriptions.add(subscription.getSubscriptionId());
                }
            }
        }

        log.info("User-specific event {} sent to {}/{} subscriptions for user: {}",
                event.getId(), successCount, userSubscriptions.size(), targetUserId);

        // Cleanup failed subscriptions
        failedSubscriptions.forEach(subscriptionRepository::remove);
    }

    @Override
    public boolean canHandle(SseEvent event) {
        return event.getScope() == EventScope.USER_SPECIFIC && event.getTargetUserId().isPresent();
    }

    private boolean sendToSubscription(SubscriptionId subscriptionId, SseEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(convertToLegacyFormat(event));
            return connectionManager.sendToConnection(subscriptionId,
                    SseEmitter.event()
                            .id(event.getId())
                            .name(event.getType().toString().toLowerCase())
                            .data(eventJson));
        } catch (Exception e) {
            log.error("Failed to send event to subscription: {}", subscriptionId, e);
            return false;
        }
    }

    private Object convertToLegacyFormat(SseEvent event) {
        return com.example.demo.model.ServerEvent.builder()
                .id(event.getId())
                .type(event.getType())
                .title(event.getTitle())
                .message(event.getMessage())
                .timestamp(event.getTimestamp())
                .severity(event.getSeverity())
                .metadata(event.getMetadata())
                .build();
    }
}