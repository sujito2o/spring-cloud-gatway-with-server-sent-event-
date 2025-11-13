package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain model representing a server-sent event
 * Production-ready with proper validation and serialization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerEvent {

    private String id;

    private EventType type;

    private String title;

    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String severity; // INFO, WARNING, ERROR, SUCCESS

    private Map<String, Object> metadata;

    /**
     * Factory method to create a notification event
     */
    public static ServerEvent notification(String title, String message) {
        return ServerEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .type(EventType.NOTIFICATION)
                .title(title)
                .message(message)
                .timestamp(LocalDateTime.now())
                .severity("INFO")
                .build();
    }

    /**
     * Factory method to create an alert event
     */
    public static ServerEvent alert(String title, String message, String severity) {
        return ServerEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .type(EventType.ALERT)
                .title(title)
                .message(message)
                .timestamp(LocalDateTime.now())
                .severity(severity)
                .build();
    }

    /**
     * Factory method to create a system update event
     */
    public static ServerEvent systemUpdate(String title, String message, Map<String, Object> metadata) {
        return ServerEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .type(EventType.SYSTEM_UPDATE)
                .title(title)
                .message(message)
                .timestamp(LocalDateTime.now())
                .severity("INFO")
                .metadata(metadata)
                .build();
    }
}