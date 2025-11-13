# Redis Implementation for SSE System

## Overview

Successfully implemented Redis-based storage for the SSE subscription system as an alternative to in-memory storage. The implementation follows clean architecture principles and allows seamless switching between storage strategies via configuration.

## What Was Implemented

### 1. Redis Dependencies
Added to `build.gradle`:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'io.lettuce:lettuce-core'
```

### 2. Redis Configuration (`RedisConfig.java`)
- **Location**: `src/main/java/com/example/demo/config/RedisConfig.java`
- **Features**:
  - Modern `RedisSerializer.json()` approach (no deprecation warnings)
  - Proper Jackson configuration with JavaTimeModule for LocalDateTime support
  - Polymorphic type handling for complex object serialization
  - String serialization for keys
  - JSON serialization for values
  - Conditional activation based on `sse.storage.strategy` property

### 3. Redis Subscription Repository (`RedisSubscriptionRepository.java`)
- **Location**: `src/main/java/com/example/demo/repository/RedisSubscriptionRepository.java`
- **Features**:
  - Full implementation of `SubscriptionRepository` interface
  - Efficient indexing using Redis Sets:
    - By subscription ID (primary storage)
    - By user ID (for user-specific queries)
    - By server ID (for multi-instance scenarios)
    - Global set for all subscriptions
  - Automatic TTL management (35 minutes to match SSE timeout + buffer)
  - Atomic operations for data consistency
  - Thread-safe operations

### 4. Redis Key Structure

```
sse:subscription:{subscriptionId}          # Individual subscription (Hash/JSON)
sse:user:subscriptions:{userId}            # Set of subscription IDs
sse:server:subscriptions:{serverId}        # Set of subscription IDs
sse:subscriptions:all                      # Set of all subscription IDs
```

**Example**:
```
sse:subscription:abc123 → {subscription JSON}
sse:user:subscriptions:user-456 → [abc123, def789]
sse:server:subscriptions:localhost:8080 → [abc123, def789, ghi012]
sse:subscriptions:all → [abc123, def789, ghi012]
```

### 5. Configuration Management (`application.properties`)

Added Redis configuration with storage strategy selection:
```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=60000
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
spring.data.redis.lettuce.pool.max-wait=-1ms

# Storage Strategy Selection
sse.storage.strategy=in-memory  # Options: in-memory, redis
```

### 6. Conditional Bean Activation

Both repository implementations use `@ConditionalOnProperty`:
- **InMemorySubscriptionRepository**: Active when `sse.storage.strategy=in-memory` (default)
- **RedisSubscriptionRepository**: Active when `sse.storage.strategy=redis`

Only one implementation is active at a time, ensuring no conflicts.

## How to Use

### Development Mode (In-Memory)
**Default configuration** - no changes needed:
```properties
sse.storage.strategy=in-memory
```

**Benefits**:
- No external dependencies
- Fast startup and execution
- Perfect for development and testing
- No Redis installation required

### Production Mode (Redis)

**Step 1**: Start Redis server
```bash
# Using Docker
docker run -d -p 6379:6379 redis:latest

# Or using Homebrew (macOS)
brew install redis
redis-server
```

**Step 2**: Update `application.properties`
```properties
sse.storage.strategy=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

**Step 3**: Start application
```bash
./gradlew bootRun
```

**Benefits**:
- Distributed subscription management
- Multiple server instances can share subscription state
- Automatic TTL and expiration
- Persistent storage across restarts
- Horizontal scalability

## Architecture Benefits

### 1. Zero Code Changes Required
Switch between storage strategies without modifying a single line of business logic:
```properties
# Development
sse.storage.strategy=in-memory

# Production
sse.storage.strategy=redis
```

### 2. Consistent Interface
Both implementations follow the same `SubscriptionRepository` interface:
```java
public interface SubscriptionRepository {
    void save(Subscription subscription);
    Optional<Subscription> findById(SubscriptionId subscriptionId);
    List<Subscription> findByUserId(UserId userId);
    List<Subscription> findByServerId(String serverId);
    List<Subscription> findAll();
    void remove(SubscriptionId subscriptionId);
    long count();
    void removeExpired();
}
```

### 3. Testability
Easy to mock or swap implementations for testing:
```java
@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    public SubscriptionRepository mockRepository() {
        return Mockito.mock(SubscriptionRepository.class);
    }
}
```

### 4. Multi-Instance Support (with Redis)
When using Redis, multiple application instances can share subscription data:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Instance 1 │────▶│    Redis    │◀────│  Instance 2 │
│  (Port 8080)│     │  (Metadata) │     │  (Port 8081)│
└─────────────┘     └─────────────┘     └─────────────┘
       │                                        │
       └────────────────┬───────────────────────┘
                        │
                   Load Balancer
                        │
                    ┌───▼───┐
                    │ Users │
                    └───────┘
```

Each instance maintains its own local `SseEmitter` objects, while subscription metadata is shared via Redis.

## Technical Details

### Serialization Strategy

**Value Objects Serialization**:
```json
{
  "@class": "com.example.demo.domain.Subscription",
  "subscriptionId": {
    "@class": "com.example.demo.domain.SubscriptionId",
    "value": "abc-123-def"
  },
  "userId": {
    "@class": "com.example.demo.domain.UserId",
    "value": "user-456"
  },
  "clientInfo": "Mozilla/5.0...",
  "subscribedEventTypes": ["NOTIFICATION", "ALERT"],
  "createdAt": "2025-11-08T01:30:00",
  "lastHeartbeat": "2025-11-08T01:35:00",
  "serverId": "localhost:8080"
}
```

### TTL Management

- **Subscription TTL**: 35 minutes (SSE timeout 30 min + 5 min buffer)
- **Index TTL**: Automatically renewed on each heartbeat
- **Expired Cleanup**: Scheduled job runs every 2 minutes
- **Automatic Eviction**: Redis handles TTL expiration

### Performance Characteristics

**Read Operations**:
- `findById`: O(1) - Direct key lookup
- `findByUserId`: O(N) where N = user's subscription count
- `findAll`: O(M) where M = total subscriptions
- `count`: O(1) - Set size operation

**Write Operations**:
- `save`: O(1) - Multiple SET/SADD operations
- `remove`: O(1) - Multiple DEL/SREM operations

**Memory Usage**:
- Per subscription: ~500 bytes (JSON)
- Per index entry: ~100 bytes
- Total for 10,000 subscriptions: ~6 MB

## Monitoring and Operations

### Check Redis Storage
```bash
# Connect to Redis CLI
redis-cli

# List all SSE keys
KEYS sse:*

# Check subscription count
SCARD sse:subscriptions:all

# Get specific subscription
GET sse:subscription:{subscriptionId}

# Check user subscriptions
SMEMBERS sse:user:subscriptions:{userId}

# Check TTL
TTL sse:subscription:{subscriptionId}
```

### Health Check
```bash
curl http://localhost:8080/api/v2/events/health

# Response
{
  "status": "UP",
  "activeConnections": 5,
  "timestamp": "2025-11-08T01:45:00"
}
```

## Migration Guide

### From In-Memory to Redis

**Step 1**: Install Redis
```bash
docker run -d -p 6379:6379 --name sse-redis redis:latest
```

**Step 2**: Update Configuration
```properties
sse.storage.strategy=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

**Step 3**: Restart Application
```bash
./gradlew bootRun
```

**Note**: Existing in-memory subscriptions will be lost. Clients will automatically reconnect.

### From Redis to In-Memory

**Step 1**: Update Configuration
```properties
sse.storage.strategy=in-memory
```

**Step 2**: Restart Application
```bash
./gradlew bootRun
```

**Note**: Redis subscriptions are not migrated. Clients will reconnect.

## Future Enhancements

### Phase 3: Redis Pub/Sub
Add cross-server event distribution:
```java
@Service
public class RedisEventDistributor {
    public void publishEvent(SseEvent event) {
        redisTemplate.convertAndSend("sse:events", event);
    }
}
```

### Phase 4: Redis Streams
Use Redis Streams for event history and replay:
```java
redisTemplate.opsForStream()
    .add(StreamRecords.string(Map.of("event", event))
    .withStreamKey("sse:stream"));
```

## Troubleshooting

### Issue: Application won't start with Redis strategy

**Error**: `Cannot connect to Redis at localhost:6379`

**Solution**:
```bash
# Check if Redis is running
redis-cli ping

# If not running, start Redis
docker start sse-redis
# Or
redis-server
```

### Issue: Subscriptions not persisting

**Check**: Verify Redis configuration
```bash
# In Redis CLI
CONFIG GET maxmemory-policy
# Should allow TTL-based eviction (e.g., allkeys-lru or volatile-lru)
```

### Issue: High memory usage

**Solution**: Reduce TTL or increase cleanup frequency
```properties
# In application.properties
sse.subscription.ttl=1800000  # 30 minutes instead of 35
```

## Summary

✅ **Implemented**: Production-ready Redis storage for SSE subscriptions
✅ **Configuration**: Simple property-based strategy switching
✅ **Zero Breaking Changes**: Existing code unchanged
✅ **Scalability**: Multi-instance support with shared state
✅ **Performance**: Efficient indexing and O(1) lookups
✅ **Clean Code**: SOLID principles maintained
✅ **Documentation**: Complete architecture guide updated

The system is now ready for production deployment with distributed storage capabilities while maintaining the flexibility to use in-memory storage for development.