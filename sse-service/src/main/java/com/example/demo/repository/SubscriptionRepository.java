package com.example.demo.repository;

import com.example.demo.domain.Subscription;
import com.example.demo.domain.SubscriptionId;
import com.example.demo.domain.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Subscription persistence
 * Follows Repository pattern and Dependency Inversion Principle
 */
public interface SubscriptionRepository {

    /**
     * Save or update a subscription
     */
    void save(Subscription subscription);

    /**
     * Find subscription by ID
     */
    Optional<Subscription> findById(SubscriptionId subscriptionId);

    /**
     * Find all subscriptions for a specific user
     */
    List<Subscription> findByUserId(UserId userId);

    /**
     * Find all active subscriptions
     */
    List<Subscription> findAll();

    /**
     * Find subscriptions on a specific server
     */
    List<Subscription> findByServerId(String serverId);

    /**
     * Remove a subscription
     */
    void remove(SubscriptionId subscriptionId);

    /**
     * Get total subscription count
     */
    long count();

    /**
     * Remove expired subscriptions
     */
    void removeExpired();
}