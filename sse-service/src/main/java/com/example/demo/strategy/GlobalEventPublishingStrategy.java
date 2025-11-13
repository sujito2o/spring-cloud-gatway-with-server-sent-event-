package com.example.demo.strategy;

import com.example.demo.domain.EventScope;
import com.example.demo.domain.SseEvent;
import com.example.demo.domain.Subscription;
import com.example.demo.domain.SubscriptionId;
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
 * Strategy for publishing global events to all subscribers
 * Single Responsibility: Handles only global event distribution
 * Uses singleton SseConnectionManager for connection management
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalEventPublishingStrategy implements EventPublishingStrategy {

    private final SubscriptionRepository subscriptionRepository;
    private final SseConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(SseEvent event) {
        log.debug("Publishing global event: {} to all subscribers", event.getType());

        List<SubscriptionId> failedSubscriptions = new ArrayList<>();
        List<Subscription> subscriptions = subscriptionRepository.findAll();

        int successCount = 0;
        for (Subscription subscription : subscriptions) {
            if (subscription.shouldReceive(event.getType(), event.getScope(), null)) {
                boolean sent = sendToSubscription(subscription.getSubscriptionId(), event);
                if (sent) {
                    successCount++;
                } else {
                    failedSubscriptions.add(subscription.getSubscriptionId());
                }
            }
        }

        log.info("Global event {} sent to {}/{} subscribers",
                event.getId(), successCount, subscriptions.size());

        // Cleanup failed subscriptions
        failedSubscriptions.forEach(subscriptionRepository::remove);
    }

    @Override
    public boolean canHandle(SseEvent event) {
        return event.getScope() == EventScope.GLOBAL;
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
        // Convert to legacy ServerEvent format for backwards compatibility
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