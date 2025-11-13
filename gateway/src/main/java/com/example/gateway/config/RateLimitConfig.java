package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate limit configuration
 * Defines how to identify clients for rate limiting
 */
@Configuration
public class RateLimitConfig {

    /**
     * Key resolver based on client IP address
     * In production, you might want to use API keys or user IDs instead
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            return Mono.just(clientIp);
        };
    }

    /**
     * Alternative: Key resolver based on userId query parameter
     * Uncomment and use this bean if you want user-based rate limiting
     */
    // @Bean
    // public KeyResolver userIdKeyResolver() {
    //     return exchange -> {
    //         String userId = exchange.getRequest().getQueryParams().getFirst("userId");
    //         return Mono.just(userId != null ? userId : "anonymous");
    //     };
    // }
}
