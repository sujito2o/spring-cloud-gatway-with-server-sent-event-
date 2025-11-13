package com.example.demo.repository;

import com.example.demo.domain.Subscription;
import com.example.demo.domain.SubscriptionId;
import com.example.demo.domain.UserId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of SubscriptionRepository
 * Thread-safe using ConcurrentHashMap
 * Suitable for single-instance deployments or development
 */
@Slf4j
@Repository
@ConditionalOnProperty(name = "sse.storage.strategy", havingValue = "in-memory", matchIfMissing = true)
public class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final ConcurrentHashMap<SubscriptionId, Subscription> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void save(Subscription subscription) {
        subscriptions.put(subscription.getSubscriptionId(), subscription);
        log.debug("Saved subscription: {} for user: {}",
                subscription.getSubscriptionId(), subscription.getUserId());
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId subscriptionId) {
        return Optional.ofNullable(subscriptions.get(subscriptionId));
    }

    @Override
    public List<Subscription> findByUserId(UserId userId) {
        return subscriptions.values().stream()
                .filter(sub -> sub.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findAll() {
        return List.copyOf(subscriptions.values());
    }

    @Override
    public List<Subscription> findByServerId(String serverId) {
        return subscriptions.values().stream()
                .filter(sub -> sub.getServerId().equals(serverId))
                .collect(Collectors.toList());
    }

    @Override
    public void remove(SubscriptionId subscriptionId) {
        Subscription removed = subscriptions.remove(subscriptionId);
        if (removed != null) {
            log.debug("Removed subscription: {}", subscriptionId);
        }
    }

    @Override
    public long count() {
        return subscriptions.size();
    }

    @Override
    public void removeExpired() {
        List<SubscriptionId> expiredIds = subscriptions.values().stream()
                .filter(Subscription::isExpired)
                .map(Subscription::getSubscriptionId)
                .collect(Collectors.toList());

        expiredIds.forEach(this::remove);

        if (!expiredIds.isEmpty()) {
            log.info("Removed {} expired subscriptions", expiredIds.size());
        }
    }
}