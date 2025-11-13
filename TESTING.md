# Testing the SSE Application

## Issue Fixed

The index.html page was not working because Thymeleaf was attempting to process JavaScript template literals (`${}` syntax) as Thymeleaf expressions, which broke the JavaScript code.

**Solution**: Added `th:inline="none"` to the script tag and the Thymeleaf namespace to the HTML tag.

## Verification Steps

### 1. Check Application is Running

The application is currently running on port 8080:
```bash
curl http://localhost:8080/api/events/health
# Should return: {"status":"UP","activeConnections":0,"timestamp":"..."}
```

### 2. Test SSE Stream

Connect to the SSE stream:
```bash
curl -N http://localhost:8080/api/events/stream?clientInfo=TestClient
# Should receive real-time events in SSE format
```

### 3. Open Browser

1. Navigate to: `http://localhost:8080`
2. The page should automatically connect to the SSE stream
3. You should see:
   - Connection Status indicator turns GREEN
   - Events Received counter incrementing
   - Live events appearing in the events container
   - Different event types with color coding:
     - Blue border: Notifications
     - Red border: Alerts
     - Green border: System Updates
     - Purple border: Connection events

### 4. Test UI Features

#### Connection Controls
- Click "Disconnect" - connection status should turn red
- Click "Connect to Stream" - connection status should turn green again
- Click "Refresh Stats" - active connections count updates
- Click "Clear Events" - events list clears

#### Event Filtering
- Uncheck "Notifications" - notification events disappear
- Uncheck "Alerts" - alert events disappear
- Uncheck "System Updates" - system update events disappear
- Re-check boxes - events reappear

### 5. Expected Event Types

You should see these types of events automatically generated:

1. **Connection Event** (on connect)
   - Title: "Connected"
   - Message: "Successfully connected to event stream"
   - Purple border

2. **Notifications** (every ~5 seconds)
   - Examples: "New user registration", "Order completed", "Payment processed"
   - Blue border
   - Severity: INFO

3. **System Updates** (every ~10 seconds)
   - Title: "System Metrics"
   - Contains metadata with CPU, memory, activeUsers, requestsPerSecond
   - Green border

4. **Alerts** (random, every ~20 seconds)
   - Examples: "High CPU usage detected", "Low disk space warning"
   - Red border
   - Severity: WARNING or ERROR

## API Endpoints

Test all endpoints:

```bash
# Health check
curl http://localhost:8080/api/events/health

# Get statistics
curl http://localhost:8080/api/events/stats

# Get event history
curl http://localhost:8080/api/events/history?limit=5

# Manually trigger an event
curl -X POST http://localhost:8080/api/events/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "type": "NOTIFICATION",
    "title": "Manual Test",
    "message": "This is a manually triggered event",
    "severity": "SUCCESS"
  }'
```

## Browser Console

Open browser DevTools Console (F12) and check for:
- "SSE connection opened" message
- No JavaScript errors
- Event objects being received and parsed

## Troubleshooting

### If events are not appearing:

1. Check browser console for errors
2. Verify the SSE connection is active (green indicator)
3. Check Network tab in DevTools for the `/api/events/stream` connection
4. Verify backend logs show "New SSE subscription created"

### If JavaScript is broken:

1. View page source and check the `<script>` tag
2. Ensure template literals like `${event.title}` are present (not processed by Thymeleaf)
3. Verify no syntax errors in console

### If connection keeps dropping:

1. Check backend logs for errors
2. Verify network stability
3. Check timeout settings in SseService.java (default: 30 minutes)

## Success Criteria

The application is working correctly when:

- ✅ Page loads without JavaScript errors
- ✅ Connection status indicator turns green automatically
- ✅ Events appear in real-time with proper formatting
- ✅ Event counter increments as events arrive
- ✅ All buttons work (Connect, Disconnect, Clear, Refresh Stats)
- ✅ Event filters work correctly
- ✅ Events show metadata (for system updates)
- ✅ Events have proper color coding by type
- ✅ Timestamps are displayed correctly
- ✅ Severity badges show correct values
- ✅ Active connections count updates every 5 seconds

## All Fixed!

The Thymeleaf template literal issue has been resolved. The JavaScript now works correctly with the `th:inline="none"` attribute preventing Thymeleaf from processing the ES6 template literals.