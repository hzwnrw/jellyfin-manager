# Jellyfin User Manager

A Spring Boot application for managing Jellyfin user accounts with automatic expiration scheduling.

## Features

- **User Management**: View all Jellyfin users in a web interface
- **Instant Disable/Enable**: Immediately disable or enable user accounts
- **Scheduled Expiration**: Set expiration dates with time precision for automatic user disabling
- **Automatic Processing**: Scheduler checks expirations at configured intervals and disables expired accounts
- **Clean UI**: Bootstrap-based responsive interface for easy management

## Prerequisites

- Java 21
- Maven 3.6+
- Jellyfin server with API access
- Spring Boot 3.x

## Configuration

Add the following properties to `src/main/resources/application.properties`:

```properties
jellyfin.url=http://your-jellyfin-server:8096
jellyfin.api-key=your-jellyfin-api-key
```

## Building

```bash
mvn clean package
```

## Running

```bash
mvn spring-boot:run
```

Or using the built JAR:

```bash
java -jar target/jellyfin-manager-1.0.0.jar
```

The application will be available at `http://localhost:8080`

## Usage

1. **View Users**: Navigate to the home page to see all Jellyfin users
2. **Disable/Enable Now**: Click "Disable Now" or "Enable Now" to immediately update a user's status
3. **Schedule Expiration**: Set a date and time in the datetime input field and click "Set" to schedule automatic disabling
4. **Automatic Processing**: The scheduler runs at configured intervals and automatically disables users whose expiration date has passed

## Scheduler Configuration

Currently set to run every 5 seconds for testing. For production, change in `ExpirationTask.java`:

```java
@Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
```