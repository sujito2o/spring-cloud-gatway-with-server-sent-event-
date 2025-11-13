package com.example.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller for circuit breaker responses
 * Provides graceful degradation when backend services are unavailable
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/sse")
    public ResponseEntity<Map<String, Object>> sseFallback() {
        log.warn("SSE stream fallback triggered - service unavailable");

        Map<String, Object> response = new HashMap<>();
        response.put("error", "SSE service temporarily unavailable");
        response.put("message", "Please try again in a few moments");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastFallback() {
        log.warn("Broadcast fallback triggered - service unavailable");

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Event broadcast service temporarily unavailable");
        response.put("message", "Your event has been queued and will be delivered when service is restored");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }

    @PostMapping("/user-event")
    public ResponseEntity<Map<String, Object>> userEventFallback() {
        log.warn("User event fallback triggered - service unavailable");

        Map<String, Object> response = new HashMap<>();
        response.put("error", "User event service temporarily unavailable");
        response.put("message", "Your event has been queued and will be delivered when service is restored");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }

    @GetMapping("/generic")
    public ResponseEntity<Map<String, Object>> genericFallback() {
        log.warn("Generic fallback triggered");

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Service temporarily unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }
}
