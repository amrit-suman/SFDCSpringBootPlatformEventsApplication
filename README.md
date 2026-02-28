# SfdcPlatformEventsApplication

Spring Boot application that subscribes to Salesforce Platform Events using the CometD / Bayeux protocol.

## Table of contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Build & Run](#build--run)
- [Project Structure](#project-structure)
- [Key classes & flow](#key-classes--flow)
- [Troubleshooting](#troubleshooting)

## Overview

This project demonstrates subscribing to Salesforce Platform Events from a Java Spring Boot application using the CometD client library. The application:

1. Authenticates to Salesforce using OAuth2 Client Credentials flow
2. Obtains an access token and instance URL
3. Constructs a CometD (Bayeux) client with proper SSL/TLS configuration
4. Establishes a secure WebSocket connection to Salesforce's CometD endpoint
5. Subscribes to platform event channels and processes incoming events in real-time

## Prerequisites

- Java 21+ (as specified in `pom.xml`)
- Maven 3.9+
- Git
- Valid Salesforce Developer Edition org with Platform Events configured
- Salesforce OAuth2 credentials (Client ID and Client Secret)

## Quick start

1. Clone the repository:
   ```bash
   git clone https://github.com/amrit-suman/SfdcPlatformEventsApplication.git
   cd SfdcPlatformEventsApplication
   ```

2. Configure Salesforce credentials (see [Configuration](#configuration) section below)

3. Build the project:
   ```bash
   ./mvnw clean package
   ```

4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

The application will start, authenticate with Salesforce, and begin listening for platform events.

## Configuration

**⚠️ Security Warning**: Never commit credentials to the repository.

### Option 1: Environment Variables (Recommended for Production)

```bash
export SALESFORCE_LOGIN_URL=https://your-instance.my.salesforce.com
export SALESFORCE_CLIENT_ID=your_client_id
export SALESFORCE_CLIENT_SECRET=your_client_secret
export SALESFORCE_API_VERSION=65.0
```

### Option 2: Local Properties File (Development Only)

Create `src/main/resources/application-local.properties` (this file is in `.gitignore`):

```properties
spring.application.name=sfdcPlatformEvents
salesforce.login-url=https://your-instance.my.salesforce.com
salesforce.client-id=your_client_id
salesforce.client-secret=your_client_secret
salesforce.api-version=65.0
```

Run with the `local` profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `salesforce.login-url` | Salesforce login endpoint | Required |
| `salesforce.client-id` | OAuth2 Client ID | Required |
| `salesforce.client-secret` | OAuth2 Client Secret | Required |
| `salesforce.api-version` | Salesforce API version | 65.0 |

## Build & Run

### Build
```bash
./mvnw clean package
```

### Run
```bash
./mvnw spring-boot:run
```

### Run Tests
```bash
./mvnw test
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/sfdcPlatformEvents/
│   │   ├── SfdcPlatformEventsApplication.java      # Spring Boot entry point
│   │   ├── SalesforceOAuthService.java             # OAuth2 authentication
│   │   └── PlatformEventSubscriber.java            # CometD client & event subscription
│   └── resources/
│       └── application.properties                  # Default configuration
└── test/
    └── java/com/example/sfdcPlatformEvents/
        └── SfdcPlatformEventsApplicationTests.java # Unit tests
```

## Key classes & flow

### [`SfdcPlatformEventsApplication.java`](src/main/java/com/example/sfdcPlatformEvents/SfdcPlatformEventsApplication.java)

**Purpose**: Spring Boot application entry point.

**Key method**:
- `main(String[] args)` — Bootstraps the Spring context and starts the application

### [`SalesforceOAuthService.java`](src/main/java/com/example/sfdcPlatformEvents/SalesforceOAuthService.java)

**Purpose**: Handles OAuth2 authentication with Salesforce using the Client Credentials flow.

**Key method**:
- `getAccessToken()` — Exchanges client credentials for an access token
    - Sends a POST request to `{login-url}/services/oauth2/token`
    - Parameters: `grant_type`, `client_id`, `client_secret`
    - Returns: Map containing `access_token` and `instance_url`
    - Error handling: Logs stack trace and returns null on failure

**Implementation details**:
- Uses Spring's `RestTemplate` for HTTP communication
- Uses `LinkedMultiValueMap` to properly encode form data and handle special characters
- Sets `Content-Type: application/x-www-form-urlencoded` header

### [`PlatformEventSubscriber.java`](src/main/java/com/example/sfdcPlatformEvents/PlatformEventSubscriber.java)

**Purpose**: Manages the CometD/Bayeux client, handles platform event subscriptions, and processes incoming events.

**Key methods**:
- `subscribe()` — Main subscription method (triggered on application startup via `@EventListener(ContextRefreshedEvent.class)`)
    - Obtains access token from `SalesforceOAuthService`
    - Extracts `access_token` and `instance_url` from OAuth response
    - Creates CometD client via `getBayeuxClient()`
    - Performs handshake with Salesforce
    - Subscribes to `/event/Order_Event__e` channel
    - Keeps connection alive with `waitFor()`

- `getBayeuxClient(String instanceUrl, String accessToken)` — Constructs the CometD client
    - Configures SSL/TLS context using Jetty's `SslContextFactory`
    - Creates `HttpClientTransportDynamic` for HTTP/2 support
    - Initializes Jetty `HttpClient` and starts it
    - Builds CometD endpoint URL: `{instanceUrl}/cometd/{apiVersion}`
    - Customizes request headers to include Bearer token authorization
    - Returns configured `BayeuxClient`

**Event handling**:
- Subscribes to `/event/Order_Event__e` channel
- Lambda callback logs received message data to console

**Lifecycle**:
- `@Async` — Runs subscription in background thread to prevent blocking Spring startup
- Connection persists until application shutdown

## Dependencies

Key dependencies (from [`pom.xml`](pom.xml)):

- **Spring Boot 4.0.3** — Application framework
- **CometD 8.0.9** — Bayeux protocol client
- **Jetty 12.0.21** — HTTP client and transport layer
- **Lombok** — Annotation processing for boilerplate reduction

## Troubleshooting

### Connection Issues

**Problem**: Handshake fails with SSL certificate errors
- **Solution**: Ensure your Salesforce org's SSL certificate is valid and trusted
- Check firewall rules allow outbound HTTPS connections to your Salesforce instance

**Problem**: "Unauthorized" error during handshake
- **Solution**: Verify that:
    - Client ID and Client Secret are correct
    - OAuth application has proper permissions
    - Access token was successfully obtained

**Problem**: Cannot find channel `/event/Order_Event__e`
- **Solution**: Ensure:
    - Platform Event named `Order_Event__e` exists in your Salesforce org
    - Channel name matches exactly (including `__e` suffix)
    - Your Salesforce user has permission to subscribe to the event

### Dependency Conflicts

**Problem**: `NoSuchMethodError` or `ClassCastException` at runtime
- **Solution**: Version compatibility issue between CometD and Jetty
- Check that `pom.xml` has matching versions (CometD 8.0.9 requires Jetty 12.0.21+)
- Run `./mvnw dependency:tree` to verify no conflicting versions

### Logging

Add debug logging by creating `src/main/resources/application.properties`:

```properties
logging.level.org.cometd=DEBUG
logging.level.org.eclipse.jetty=DEBUG
logging.level.com.example.sfdcPlatformEvents=DEBUG
```

## Next Steps

- Add event processing logic to handle received messages
- Implement error handling and reconnect with exponential backoff
- Add metrics/monitoring for connection health
- Support subscribing to multiple platform event channels
- Implement graceful shutdown handling

## License

This project is provided as-is for educational and development purposes.