# Spring Cloud Gateway - Production-Ready Setup

## Architecture Overview

This project uses a microservices architecture with Spring Cloud Gateway as the API Gateway.

```
┌─────────────┐         ┌──────────────────┐         ┌──────────────┐
│   Client    │────────▶│   API Gateway    │────────▶│ SSE Service  │
│   (UI)      │         │   (Port 8080)    │         │ (Port 8081)  │
└─────────────┘         └──────────────────┘         └──────────────┘
                               │
                               │
                        ┌──────▼──────┐
                        │    Redis    │
                        │ (Port 6379) │
                        └─────────────┘
```

## Modules

### 1. Gateway (`gateway/`)
- **Port**: 8080
- **Purpose**: API Gateway with production-ready features
- **Technology**: Spring Cloud Gateway (Reactive)

### 2. SSE Service (`sse-service/`)
- **Port**: 8081
- **Purpose**: Server-Sent Events backend service
- **Technology**: Spring Boot MVC

## Production-Ready Features

### 1. Rate Limiting
- **Implementation**: Redis-based distributed rate limiting
- **Configuration**: Per-route limits defined in `gateway/src/main/resources/application.yml`
- **Key Resolver**: IP-based (configurable to user-based)

#### Rate Limits per Endpoint:
- `/api/v2/events/stream` - 10 req/sec, burst 20
- `/api/v2/events/broadcast` - 5 req/sec, burst 10
- `/api/v2/events/send/{userId}` - 5 req/sec, burst 10
- `/api/v2/events/stats/**` - 20 req/sec, burst 40
- `/api/v2/events/health` - 50 req/sec, burst 100

### 2. Circuit Breaker
- **Implementation**: Resilience4j
- **Configuration**:
  - Sliding window size: 10 requests
  - Failure rate threshold: 50%
  - Wait duration in open state: 10s
  - Half-open state: 3 permitted calls
- **Fallback**: Custom fallback endpoints for graceful degradation

### 3. Security Headers
All responses include:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `Cache-Control: no-cache, no-store, must-revalidate`

### 4. CORS Configuration
- **Global CORS**: Configured for all routes
- **Allowed Origins**: * (configure for production)
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Max Age**: 3600s

### 5. Request Tracing
- **X-Request-ID**: Unique ID for each request
- **X-Request-Time-Ms**: Request processing time
- **Logging**: Full request/response logging with correlation IDs

### 6. Retry Mechanism
- **Retries**: 3 attempts
- **Backoff**: Exponential (50ms to 500ms)
- **Status Codes**: BAD_GATEWAY, SERVICE_UNAVAILABLE
- **Methods**: GET only

### 7. Monitoring & Observability
- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`, `/actuator/gateway`
- **Prometheus Metrics**: Enabled for both services
- **Health Checks**: Circuit breaker and rate limiter health

## Running the Application

### Prerequisites
1. Java 21+
2. Redis running on localhost:6379
3. Gradle 8.x

### Start Redis
```bash
# Using Docker
docker run -d -p 6379:6379 redis:latest

# Or using local Redis
redis-server
```

### Build All Modules
```bash
./gradlew clean build
```

### Run Services

#### Option 1: Run Both Services Separately
```bash
# Terminal 1 - Start SSE Service
./gradlew :sse-service:bootRun

# Terminal 2 - Start Gateway
./gradlew :gateway:bootRun
```

#### Option 2: Build and Run JARs
```bash
# Build JARs
./gradlew build

# Run SSE Service
java -jar sse-service/build/libs/sse-service-0.0.1-SNAPSHOT.jar

# Run Gateway
java -jar gateway/build/libs/gateway-0.0.1-SNAPSHOT.jar
```

### Access the Application
- **UI**: http://localhost:8080/
- **Gateway Health**: http://localhost:8080/actuator/health
- **Gateway Metrics**: http://localhost:8080/actuator/metrics
- **Gateway Routes**: http://localhost:8080/actuator/gateway/routes
- **SSE Service Health**: http://localhost:8081/actuator/health

## Testing Production Features

### 1. Test Rate Limiting
```bash
# Trigger rate limit (send > 10 requests/sec)
for i in {1..25}; do
  curl -w "\n%{http_code}\n" http://localhost:8080/api/v2/events/health
done
```

### 2. Test Circuit Breaker
```bash
# Stop SSE service to trigger circuit breaker
# Gateway will return fallback responses after failure threshold
curl http://localhost:8080/api/v2/events/health
```

### 3. Test Request Tracing
```bash
# Check X-Request-ID and X-Request-Time-Ms headers
curl -v http://localhost:8080/api/v2/events/health
```

### 4. Monitor Metrics
```bash
# Gateway metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## Configuration

### Gateway Configuration
Location: `gateway/src/main/resources/application.yml`

Key configurations:
- Route definitions
- Rate limiting
- Circuit breaker
- CORS
- Security headers
- Actuator endpoints

### SSE Service Configuration
Location: `sse-service/src/main/resources/application.properties`

Key configurations:
- Server port (8081)
- Redis connection
- Storage strategy
- Actuator endpoints

## Production Deployment Checklist

- [ ] Update CORS allowed origins to specific domains
- [ ] Configure Redis with authentication
- [ ] Use environment-specific configuration profiles
- [ ] Set up centralized logging (ELK, Splunk, etc.)
- [ ] Configure SSL/TLS certificates
- [ ] Set up load balancer in front of gateway
- [ ] Configure service discovery (Eureka, Consul)
- [ ] Set up distributed tracing (Zipkin, Jaeger)
- [ ] Configure API authentication/authorization
- [ ] Set up monitoring dashboards (Grafana)
- [ ] Configure backup gateway instances
- [ ] Set up alerting for circuit breaker state changes
- [ ] Review and adjust rate limits based on load testing
- [ ] Configure health check endpoints for load balancer
- [ ] Set up log aggregation
- [ ] Configure API documentation (Swagger/OpenAPI)

## Troubleshooting

### Redis Connection Issues
```bash
# Check if Redis is running
redis-cli ping

# Expected response: PONG
```

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Gateway Not Routing
Check gateway routes:
```bash
curl http://localhost:8080/actuator/gateway/routes
```

### Circuit Breaker Not Working
Check circuit breaker status:
```bash
curl http://localhost:8080/actuator/health
```

## Logs
- **Gateway logs**: `logs/gateway.log`
- **SSE Service logs**: `logs/sse-service.log`

## Performance Tuning

### Gateway JVM Options
```bash
java -Xms512m -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar gateway/build/libs/gateway-0.0.1-SNAPSHOT.jar
```

### SSE Service JVM Options
```bash
java -Xms512m -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar sse-service/build/libs/sse-service-0.0.1-SNAPSHOT.jar
```

## Security Considerations

1. **API Keys**: Implement API key validation in gateway
2. **JWT**: Add JWT validation for authenticated endpoints
3. **IP Whitelisting**: Configure IP-based access control
4. **DDoS Protection**: Use rate limiting and circuit breakers
5. **Input Validation**: Validate all inputs at gateway level
6. **SSL/TLS**: Enable HTTPS in production

## Monitoring Best Practices

1. Monitor circuit breaker state transitions
2. Track rate limit rejections
3. Monitor request latency (X-Request-Time-Ms)
4. Set up alerts for high error rates
5. Monitor Redis connection pool
6. Track gateway throughput
7. Monitor memory and CPU usage

## Support

For issues or questions, refer to:
- Spring Cloud Gateway: https://spring.io/projects/spring-cloud-gateway
- Resilience4j: https://resilience4j.readme.io/
- Spring Boot Actuator: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
