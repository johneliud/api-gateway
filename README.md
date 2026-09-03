# API Gateway

Spring Cloud Gateway service that acts as the single entry point for all client requests, routing them to the appropriate backend microservices.

## Overview

- **Port**: 8080
- **Technology**: Spring Boot 4.0.3 / Spring Cloud Gateway (Reactive / WebFlux)
- **Purpose**: Request routing, authentication, and cross-cutting concerns

## Features

### Request Routing
Routes requests to backend services

### Authentication
- JWT token validation for protected routes
- Extracts userId and role from token
- Forwards user context via headers (X-User-Id, X-User-Role)
- Returns 401 for invalid/expired tokens

### Rate Limiting
- Login endpoint: 5 attempts per 15 minutes per IP
- Uses Bucket4j for in-memory rate limiting
- Returns 429 Too Many Requests when exceeded

### Security Headers
All responses include:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Content-Security-Policy`

### CORS Configuration
- Allowed origins
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials enabled

### Response Handling
- Binary responses (images) handled as byte arrays
- JSON responses handled as strings
- Query parameters forwarded to backend services

## Configuration

### Application Properties
```properties
server.port=8080
jwt.secret=your-secret-key
```

### Route Configuration
Routes are defined in `RouteConfig.java`:
- Public routes: register, login, get products, get media
- Protected routes: profile, product CRUD, media upload/delete

## Running the Service

```bash
cd backend/api-gateway
mvn spring-boot:run
```

Service will start on port 8080.

## API Endpoints

All requests go through the gateway at `http://localhost:8080`

## Security

- JWT tokens validated at gateway
- User context forwarded to backend services
- Backend services trust X-User-Id and X-User-Role headers
- Rate limiting prevents brute force attacks

## Dependencies

- Spring Boot 4.0.3
- Spring Cloud Gateway (WebFlux)
- JWT (io.jsonwebtoken / jjwt)
- Bucket4j (rate limiting)
- Lombok

## Error Responses

```json
{
  "error": "Error message"
}
```

Status codes:
- 401 - Unauthorized (invalid/missing token)
- 429 - Too Many Requests (rate limit exceeded)
- 502 - Bad Gateway (backend service unavailable)
