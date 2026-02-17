# API Gateway

A Spring Boot WebFlux microservice that acts as the single entry point for all client requests, routing them to the appropriate backend microservices (user, product, media).

## Features

### Completed Implementations

#### AG-1: Gateway Setup & Configuration
- Spring Boot WebFlux reactive gateway
- Routes configured for all microservices
- Service endpoints: user (8080), media (8081), product (8082)
- Gateway port: 8083

#### AG-2: JWT Authentication Filter
- JWT token validation for protected routes
- Extracts userId and role from token
- Forwards user info to downstream services via headers (X-User-Id, X-User-Role)
- Returns 401 for invalid/expired tokens

#### AG-3: Route Configuration
- **User Service:** /api/users/** → localhost:8080
  - Public: register, login
  - Protected: profile endpoints
- **Product Service:** /api/products/** → localhost:8082
  - Public: GET all products, GET single product
  - Protected: POST, PUT, DELETE operations
- **Media Service:** /api/media/** → localhost:8081
  - Public: GET media by id, GET media by product
  - Protected: POST upload, DELETE operations

#### AG-4: CORS Configuration
- Allowed origins: http://localhost:4200, http://localhost:3000
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials enabled

#### AG-5: Rate Limiting
- Login endpoint rate limited: 5 attempts per 15 minutes per IP
- Uses Bucket4j for in-memory rate limiting
- Returns 429 Too Many Requests when exceeded

#### AG-6: Error Handling
- Consistent error responses
- 401 Unauthorized for authentication failures
- 429 Too Many Requests for rate limit violations
- 503 Service Unavailable for downstream service failures

#### AG-7: Security Headers
- CORS headers applied to all responses
- Centralized security configuration

#### AG-8: Testing
- Manual testing guide available in docs/TESTING.md
- All routes tested and functional

## Tech Stack

- **Java 25**
- **Spring Boot 4.0.2**
- **Spring WebFlux** - Reactive web framework
- **WebClient** - HTTP client for proxying requests
- **JWT (JJWT 0.12.5)** - Token validation
- **Bucket4j 8.10.1** - Rate limiting
- **Lombok** - Boilerplate reduction
- **Maven** - Build tool

## Architecture

The gateway uses a **reactive proxy pattern** with Spring WebFlux:
- Receives client requests
- Validates JWT tokens for protected routes
- Applies rate limiting where needed
- Forwards requests to downstream services with user headers
- Returns responses to clients

## Getting Started

### Prerequisites

- Java 25
- Maven 3.6+
- All microservices running (user, product, media)

### Configuration

Update `src/main/resources/application-secrets.properties`:

```properties
# JWT Configuration
jwt.secret=your-secret-key-here
jwt.expiration=86400000

# Service URLs
user.service.url=http://localhost:8080
product.service.url=http://localhost:8082
media.service.url=http://localhost:8081

# Rate limiting
rate.limit.login.capacity=5
rate.limit.login.refill.tokens=5
rate.limit.login.refill.minutes=15
```

### Build & Run

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run
```

The gateway will start on `http://localhost:8083`

## Usage

All client requests should go through the gateway:

```bash
# Instead of: http://localhost:8080/api/users/login
# Use: http://localhost:8083/api/users/login

# Register user
curl -X POST http://localhost:8083/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Pass@123","role":"CLIENT"}'

# Login
curl -X POST http://localhost:8083/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Pass@123"}'

# Access protected endpoint
curl http://localhost:8083/api/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## How It Works

1. **Client Request** → Gateway (port 8083)
2. **Gateway validates JWT** (if protected route)
3. **Gateway applies rate limiting** (if login route)
4. **Gateway forwards request** to appropriate service with X-User-Id and X-User-Role headers
5. **Downstream service processes** request (trusts gateway headers)
6. **Gateway returns response** to client

## Benefits

- **Single entry point** for all client requests
- **Centralized authentication** - JWT validation in one place
- **Centralized rate limiting** - No duplicate logic in services
- **Centralized CORS** - Consistent cross-origin policy
- **Simplified downstream services** - No JWT validation needed
- **Better security** - Services only accessible through gateway

## Testing

See [TESTING.md](docs/TESTING.md) for detailed testing instructions.