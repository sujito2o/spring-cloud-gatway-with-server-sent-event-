# Server-Sent Events (SSE) Application

A production-ready Spring Boot application demonstrating real-time event streaming using Server-Sent Events (SSE) with a modern web UI.

## Features

- **Real-time Event Streaming**: Server-Sent Events implementation for push notifications
- **Multiple Event Types**: Support for notifications, alerts, system updates, and custom events
- **Modern Web UI**: Beautiful, responsive dashboard with real-time event display
- **Connection Management**: Thread-safe subscription handling with automatic cleanup
- **Event History**: Maintains last 100 events with retrieval API
- **Production-Ready**: Includes proper logging, error handling, and configuration
- **Event Filtering**: Client-side filtering by event type
- **Auto-Generated Events**: Demo event generator for testing

## Tech Stack

- **Backend**: Spring Boot 4.0, Java 21
- **Frontend**: Vanilla JavaScript, HTML5, CSS3
- **Build Tool**: Gradle
- **Dependencies**: Lombok, Jackson, Thymeleaf

## Project Structure

```
src/main/java/com/example/demo/
├── config/
│   ├── AsyncConfig.java          # Async/scheduling configuration
│   ├── JacksonConfig.java         # JSON serialization configuration
│   └── WebConfig.java             # CORS and web configuration
├── controller/
│   ├── HomeController.java        # UI page controller
│   └── SseController.java         # SSE REST endpoints
├── exception/
│   └── GlobalExceptionHandler.java # Global error handling
├── model/
│   ├── EventType.java             # Event type enum
│   ├── ServerEvent.java           # Event domain model
│   └── EventSubscription.java     # Subscription tracking model
├── service/
│   ├── SseService.java            # Core SSE service
│   └── EventGeneratorService.java # Demo event generator
└── DemoApplication.java           # Main application class

src/main/resources/
├── application.properties         # Application configuration
└── templates/
    └── index.html                 # Main dashboard UI
```

## API Endpoints

### SSE Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/events/stream` | GET | SSE stream endpoint (EventSource compatible) |
| `/api/events/stats` | GET | Get current connection statistics |
| `/api/events/history` | GET | Get recent event history (default: 10, max: 100) |
| `/api/events/trigger` | POST | Manually trigger a custom event |
| `/api/events/health` | GET | Health check endpoint |

### SSE Stream Query Parameters

- `clientInfo` (optional): Client identifier for debugging
- `types` (optional): Array of event types to subscribe to (NOTIFICATION, ALERT, SYSTEM_UPDATE, etc.)

Example:
```
GET /api/events/stream?clientInfo=Dashboard&types=ALERT,NOTIFICATION
```

## Running the Application

### Prerequisites

- Java 21 or higher
- Gradle (or use the included wrapper)

### Build and Run

```bash
# Build the project
./gradlew clean build

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### Access the Dashboard

Open your browser and navigate to:
```
http://localhost:8080
```

You should see the Server-Sent Events Dashboard with real-time events streaming automatically.

## Configuration

### application.properties

Key configuration options:

```properties
# Server Configuration
server.port=8080

# Logging
logging.level.com.example.demo=DEBUG

# Async Thread Pool
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
```

### SSE Configuration

The SSE timeout is configured in `SseService.java`:

```java
private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
```

## Event Types

The application supports the following event types:

- **NOTIFICATION**: General notifications
- **SYSTEM_UPDATE**: System metrics and updates
- **ALERT**: Warning and error alerts
- **USER_ACTION**: User activity events
- **METRICS**: Performance metrics

## Production Considerations

### 1. CORS Configuration

Update `WebConfig.java` to specify allowed origins in production:

```java
.allowedOriginPatterns("https://yourdomain.com")
```

### 2. Event Generator

Disable the auto-generated events in production by removing or disabling the `@Scheduled` methods in `EventGeneratorService.java`, or configure them via application properties.

### 3. Logging

Adjust logging levels in `application.properties`:

```properties
logging.level.root=WARN
logging.level.com.example.demo=INFO
```

### 4. Connection Limits

Monitor active connections and adjust thread pool sizes based on your load:

```properties
spring.task.execution.pool.max-size=20
```

### 5. Security

- Add authentication/authorization to SSE endpoints
- Implement rate limiting
- Add HTTPS in production
- Configure proper CORS policies

## Testing the Application

### Using cURL

Test the health endpoint:
```bash
curl http://localhost:8080/api/events/health
```

Test the SSE stream:
```bash
curl -N http://localhost:8080/api/events/stream
```

Trigger a custom event:
```bash
curl -X POST http://localhost:8080/api/events/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "type": "NOTIFICATION",
    "title": "Test Event",
    "message": "This is a test event",
    "severity": "INFO"
  }'
```

### Using the Web UI

1. Open `http://localhost:8080` in your browser
2. Click "Connect to Stream" (auto-connects on page load)
3. Watch real-time events appear in the dashboard
4. Use filters to show/hide specific event types
5. Click "Refresh Stats" to see connection statistics
6. Click "Clear Events" to clear the event history

## Architecture Highlights

### Thread-Safe Implementation

- Uses `ConcurrentHashMap` for managing subscriptions
- `CopyOnWriteArrayList` for event history
- Async broadcasting with dedicated thread pool

### Connection Management

- Automatic cleanup on connection close/timeout/error
- Graceful handling of failed subscriptions
- Support for multiple concurrent connections

### Event Broadcasting

- Efficient async broadcasting to all subscribers
- Type-based event filtering
- Event history with size limits

### Error Handling

- Global exception handler
- Proper HTTP status codes
- Detailed error messages

## Monitoring

The application provides several monitoring capabilities:

- Active connection count via `/api/events/stats`
- Health check endpoint at `/api/events/health`
- Detailed logging with DEBUG level
- Event history tracking

## Browser Compatibility

The application uses the EventSource API, which is supported by:

- Chrome 6+
- Firefox 6+
- Safari 5+
- Edge 79+
- Opera 11+

## License

This is a demo application for educational purposes.

## Support

For issues or questions, refer to the Spring Boot documentation:
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)

## Future Enhancements

- Add authentication and authorization
- Implement event persistence
- Add WebSocket fallback
- Create admin panel for event management
- Add metrics and monitoring dashboard
- Implement event replay functionality
- Add support for private channels/rooms