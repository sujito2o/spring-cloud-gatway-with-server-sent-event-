package com.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter for logging all incoming requests and outgoing responses
 * Adds a unique request ID for tracing
 */
@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or extract request ID
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        // Add request ID to exchange attributes for use in other filters
        exchange.getAttributes().put(REQUEST_ID_HEADER, requestId);

        // Add request ID to response headers
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        // Log request details
        log.info(">>> Incoming Request [{}] | Method: {} | Path: {} | RemoteAddress: {}",
            requestId,
            request.getMethod(),
            request.getPath(),
            request.getRemoteAddress());

        // Make requestId final for use in lambda
        final String finalRequestId = requestId;

        return chain.filter(exchange).then(
            Mono.fromRunnable(() -> {
                log.info("<<< Outgoing Response [{}] | Status: {}",
                    finalRequestId,
                    exchange.getResponse().getStatusCode());
            })
        );
    }

    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }
}
