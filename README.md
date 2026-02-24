# API Gateway

Spring Cloud Gateway service that acts as the single entry point for all client requests, routing them to the appropriate backend microservices.

## Overview

- **Port**: 8083
- **Technology**: Spring Cloud Gateway (Reactive)
- **Purpose**: Request routing, authentication, and cross-cutting concerns

## Features

### Request Routing
Routes requests to backend services:
- `/api/users/**` → User Service (8080)
- `/api/products/**` → Product Service (8082)
- `/api/media/**` → Media Service (8081)
- `/api/users/avatars/**` → User Service (8080)

### Authentication
- JWT token validation for protected routes
- Extracts userId and role from token
- Forwards user context via headers (X-User-Id, X-User-Role)
- Returns 401 for invalid/expired tokens

### Rate Limiting
- Login endpoint: 5 attempts per 15 minutes per IP
- Uses Bucket4j for in-memory rate limiting
- Returns 429 Too Many Requests when exceeded

### CORS Configuration
- Allowed origins: http://localhost:4200
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials enabled

### Response Handling
- Binary responses (images) handled as byte arrays
- JSON responses handled as strings
- Query parameters forwarded to backend services

## Configuration

### Application Properties
```properties
server.port=8083
user.service.url=http://localhost:8080
product.service.url=http://localhost:8082
media.service.url=http://localhost:8081
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

Service will start on port 8083.

## API Endpoints

All requests go through the gateway at `http://localhost:8083`

### Public Endpoints
- POST `/api/users/register` - User registration
- POST `/api/users/login` - User login (rate limited)
- GET `/api/products` - Get all products
- GET `/api/media/{id}` - Get media by ID

### Protected Endpoints
Require `Authorization: Bearer <token>` header:
- GET `/api/users/profile` - Get user profile
- PUT `/api/users/profile` - Update profile
- POST `/api/products` - Create product
- POST `/api/media/upload` - Upload media

## Security

- JWT tokens validated at gateway
- User context forwarded to backend services
- Backend services trust X-User-Id and X-User-Role headers
- Rate limiting prevents brute force attacks

## Dependencies

- Spring Boot 3.x
- Spring Cloud Gateway
- JWT (io.jsonwebtoken)
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
