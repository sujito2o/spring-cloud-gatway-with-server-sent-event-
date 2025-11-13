package com.example.demo.repository;

import com.example.demo.domain.Subscription;
import com.example.demo.domain.SubscriptionId;
import com.example.demo.domain.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis-based implementation of SubscriptionRepository
 * Production-ready for multi-instance deployments
 * Implements Repository Pattern with Redis as backing store
 */
@Slf4j
@Repository
@ConditionalOnProperty(name = "sse.storage.strategy", havingValue = "redis")
@RequiredArgsConstructor
public class RedisSubscriptionRepository implements SubscriptionRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SUBSCRIPTION_KEY_PREFIX = "sse:subscription:";
    private static final String USER_SUBSCRIPTIONS_KEY_PREFIX = "sse:user:subscriptions:";
    private static final String SERVER_SUBSCRIPTIONS_KEY_PREFIX = "sse:server:subscriptions:";
    private static final String ALL_SUBSCRIPTIONS_KEY = "sse:subscriptions:all";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(35); // 5 min buffer over SSE timeout

    @Override
    public void save(Subscription subscription) {
        String key = getSubscriptionKey(subscription.getSubscriptionId());

        // Save subscription with TTL
        redisTemplate.opsForValue().set(key, subscription, DEFAULT_TTL);

        // Add to user index
        String userKey = getUserSubscriptionsKey(subscription.getUserId());
        redisTemplate.opsForSet().add(userKey, subscription.getSubscriptionId().toString());
        redisTemplate.expire(userKey, DEFAULT_TTL);

        // Add to server index
        String serverKey = getServerSubscriptionsKey(subscription.getServerId());
        redisTemplate.opsForSet().add(serverKey, subscription.getSubscriptionId().toString());
        redisTemplate.expire(serverKey, DEFAULT_TTL);

        // Add to global set
        redisTemplate.opsForSet().add(ALL_SUBSCRIPTIONS_KEY, subscription.getSubscriptionId().toString());

        log.debug("Saved subscription to Redis: {}", subscription.getSubscriptionId());
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId subscriptionId) {
        String key = getSubscriptionKey(subscriptionId);
        Object value = redisTemplate.opsForValue().get(key);

        if (value instanceof Subscription) {
            return Optional.of((Subscription) value);
        }

        return Optional.empty();
    }

    @Override
    public List<Subscription> findByUserId(UserId userId) {
        String userKey = getUserSubscriptionsKey(userId);
        Set<Object> subscriptionIds = redisTemplate.opsForSet().members(userKey);

        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            return List.of();
        }

        return subscriptionIds.stream()
                .map(id -> SubscriptionId.from(id.toString()))
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findByServerId(String serverId) {
        String serverKey = getServerSubscriptionsKey(serverId);
        Set<Object> subscriptionIds = redisTemplate.opsForSet().members(serverKey);

        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            return List.of();
        }

        return subscriptionIds.stream()
                .map(id -> SubscriptionId.from(id.toString()))
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findAll() {
        Set<Object> subscriptionIds = redisTemplate.opsForSet().members(ALL_SUBSCRIPTIONS_KEY);

        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            return List.of();
        }

        return subscriptionIds.stream()
                .map(id -> SubscriptionId.from(id.toString()))
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public void remove(SubscriptionId subscriptionId) {
        // Find subscription to get userId and serverId for index cleanup
        Optional<Subscription> optionalSubscription = findById(subscriptionId);

        if (optionalSubscription.isEmpty()) {
            log.warn("Attempting to remove non-existent subscription: {}", subscriptionId);
            return;
        }

        Subscription subscription = optionalSubscription.get();

        // Remove from main storage
        String key = getSubscriptionKey(subscriptionId);
        redisTemplate.delete(key);

        // Remove from user index
        String userKey = getUserSubscriptionsKey(subscription.getUserId());
        redisTemplate.opsForSet().remove(userKey, subscriptionId.toString());

        // Remove from server index
        String serverKey = getServerSubscriptionsKey(subscription.getServerId());
        redisTemplate.opsForSet().remove(serverKey, subscriptionId.toString());

        // Remove from global set
        redisTemplate.opsForSet().remove(ALL_SUBSCRIPTIONS_KEY, subscriptionId.toString());

        log.debug("Removed subscription from Redis: {}", subscriptionId);
    }

    @Override
    public long count() {
        Long count = redisTemplate.opsForSet().size(ALL_SUBSCRIPTIONS_KEY);
        return count != null ? count : 0L;
    }

    @Override
    public void removeExpired() {
        List<Subscription> allSubscriptions = findAll();

        long expiredCount = allSubscriptions.stream()
                .filter(Subscription::isExpired)
                .peek(subscription -> {
                    log.info("Removing expired subscription: {}", subscription.getSubscriptionId());
                    remove(subscription.getSubscriptionId());
                })
                .count();

        if (expiredCount > 0) {
            log.info("Removed {} expired subscriptions from Redis", expiredCount);
        }
    }

    // Helper methods for key generation

    private String getSubscriptionKey(SubscriptionId subscriptionId) {
        return SUBSCRIPTION_KEY_PREFIX + subscriptionId.toString();
    }

    private String getUserSubscriptionsKey(UserId userId) {
        return USER_SUBSCRIPTIONS_KEY_PREFIX + userId.toString();
    }

    private String getServerSubscriptionsKey(String serverId) {
        return SERVER_SUBSCRIPTIONS_KEY_PREFIX + serverId;
    }
}