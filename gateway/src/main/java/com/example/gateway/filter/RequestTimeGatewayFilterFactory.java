package com.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Custom Gateway Filter to measure request processing time
 * and add it to response headers
 */
@Slf4j
@Component
public class RequestTimeGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final String REQUEST_TIME_START = "requestTimeStart";
    private static final String X_REQUEST_TIME_HEADER = "X-Request-Time-Ms";

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            exchange.getAttributes().put(REQUEST_TIME_START, System.currentTimeMillis());

            return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    Long startTime = exchange.getAttribute(REQUEST_TIME_START);
                    if (startTime != null) {
                        long executionTime = System.currentTimeMillis() - startTime;
                        exchange.getResponse().getHeaders().add(X_REQUEST_TIME_HEADER, executionTime + "ms");

                        log.info("Path: {} | Method: {} | Status: {} | Time: {}ms",
                            exchange.getRequest().getPath(),
                            exchange.getRequest().getMethod(),
                            exchange.getResponse().getStatusCode(),
                            executionTime);
                    }
                })
            );
        };
    }
}
