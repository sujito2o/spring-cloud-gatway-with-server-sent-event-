package com.example.demo.service;

import com.example.demo.model.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Service to generate demo events for testing
 * In production, this would be replaced with actual business events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventGeneratorService {

    private final SseOrchestrationService orchestrationService;
    private final Random random = new Random();

    private final String[] notifications = {
            "New user registration",
            "Order completed",
            "Payment processed",
            "Report generated",
            "Backup completed"
    };

    private final String[] alerts = {
            "High CPU usage detected",
            "Low disk space warning",
            "Failed login attempt",
            "API rate limit reached",
            "Database connection slow"
    };

    /**
     * Generates periodic notification events
     * Can be disabled in production via configuration
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void generateNotificationEvent() {
        if (orchestrationService.getActiveConnectionCount() > 0) {
            String message = notifications[random.nextInt(notifications.length)];
            orchestrationService.broadcastGlobalEvent(
                    EventType.NOTIFICATION,
                    "System Notification",
                    message,
                    null
            );
            log.debug("Generated notification event: {}", message);
        }
    }

    /**
     * Generates periodic system update events with metrics
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 15000)
    public void generateSystemUpdateEvent() {
        if (orchestrationService.getActiveConnectionCount() > 0) {
            int cpu = random.nextInt(100);
            int memory = random.nextInt(100);
            int activeUsers = random.nextInt(1000);
            int requestsPerSecond = random.nextInt(500);

            String message = String.format(
                    "CPU: %d%%, Memory: %d%%, Active Users: %d, Requests/sec: %d",
                    cpu, memory, activeUsers, requestsPerSecond
            );

            orchestrationService.broadcastGlobalEvent(
                    EventType.SYSTEM_UPDATE,
                    "System Metrics",
                    message,
                    null
            );
            log.debug("Generated system update event with metrics");
        }
    }

    /**
     * Generates occasional alert events
     */
    @Scheduled(fixedDelay = 20000, initialDelay = 20000)
    public void generateAlertEvent() {
        if (orchestrationService.getActiveConnectionCount() > 0 && random.nextBoolean()) {
            String message = alerts[random.nextInt(alerts.length)];
            String severity = random.nextBoolean() ? "WARNING" : "ERROR";

            orchestrationService.broadcastGlobalEvent(
                    EventType.ALERT,
                    "System Alert",
                    message,
                    severity
            );
            log.debug("Generated alert event: {} [{}]", message, severity);
        }
    }
}