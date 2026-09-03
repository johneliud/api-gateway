# API Gateway

A generic, reusable API Gateway built with Spring Cloud Gateway. This gateway can be deployed across multiple microservices projects **without modifying its source code**.

## Architecture

```
                    CLIENT
                      │
                      ▼
              ┌───────────────┐
              │  API Gateway  │
              │               │
              │ Routing       │
              │ Security      │
              │ CORS          │
              │ Rate Limiting │
              │ Logging       │
              │ Error Handling│
              └───────┬───────┘
                      │
          Project-specific services
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       Service A   Service B   Service C
```

The client communicates only with the gateway. The gateway forwards requests to internal services. The client never needs to know internal service URLs.

## Key Features

### Generic Gateway Engine
- **Request Routing** - Routes requests to backend services based on configuration
- **JWT Authentication** - Validates JWT tokens for protected routes
- **Rate Limiting** - IP-based rate limiting using Bucket4j
- **Security Headers** - Adds security headers to all responses
- **CORS Configuration** - Configurable allowed origins
- **Error Handling** - Returns structured error responses

### Project-Specific Configuration
- Routes are defined externally (no hardcoded routes in source code)
- Configuration supplied via environment variables or mounted files
- Same gateway build works with different projects
- No source code modifications needed for new projects

## How It Works

### The Gateway Build

```
api-gateway:1.0.0
```

This single build can be used by multiple projects:

```
Project A → api-gateway:1.0.0 + Project A configuration
Project B → api-gateway:1.0.0 + Project B configuration
Project C → api-gateway:1.0.0 + Project C configuration
```

The gateway source repository never needs to know what services Project A, B, or C contains.

### Configuration Model

**Gateway Code** (immutable):
- Routing logic
- Authentication logic
- Rate limiting logic
- Security headers
- Error handling

**Runtime Configuration** (project-specific):
- Route definitions
- Service URLs
- JWT secrets
- CORS origins
- Rate limit settings

## Quick Start

### Running Locally

```bash
# Build the gateway
mvn clean package -DskipTests

# Run with default configuration
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar

# Or run with project-specific configuration
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar \
  --spring.config.location=file:./config/application-project-a.yml
```

### Running with Docker

```bash
# Build the Docker image
docker build -t api-gateway:1.0.0 .

# Run with environment variables
docker run -p 8080:8080 \
  -e JWT_SECRET=my-secret \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  -e USER_SERVICE_URL=http://user-service:8081 \
  api-gateway:1.0.0

# Run with mounted configuration
docker run -p 8080:8080 \
  -v ./my-project-config:/config \
  api-gateway:1.0.0
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Secret key for JWT validation | `change-me-in-production` |
| `JWT_EXPIRATION` | JWT token expiration (ms) | `86400000` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:4200` |
| `RATE_LIMIT_LOGIN_CAPACITY` | Rate limit capacity | `5` |
| `RATE_LIMIT_LOGIN_REFILL_TOKENS` | Rate limit refill tokens | `5` |
| `RATE_LIMIT_LOGIN_REFILL_MINUTES` | Rate limit refill interval (min) | `15` |
| `SERVER_PORT` | Gateway server port | `8080` |

### Route Configuration

Routes are defined using Spring Cloud Gateway's property-based configuration. Supply routes via:

1. **External YAML file** mounted at `/config/application.yml`
2. **Environment variables** (see Spring Cloud Gateway docs)
3. **Spring Cloud Config Server**

#### Example Route Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: ${USER_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/api/users/**
          filters:
            - name: AuthenticationFilter
        
        - id: payment-service
          uri: ${PAYMENT_SERVICE_URL:http://localhost:8082}
          predicates:
            - Path=/api/payments/**
          filters:
            - name: AuthenticationFilter
            - name: RateLimitGatewayFilter
```

#### Available Filters

- `AuthenticationFilter` - JWT token validation
- `RateLimitGatewayFilter` - IP-based rate limiting
- `SecurityHeadersFilter` - Adds security headers (applied to all responses)

### Example: Two Projects, One Gateway Build

**Project A configuration** (`config/application-project-a.yml`):
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8081
          predicates:
            - Path=/api/users/**
```

**Project B configuration** (`config/application-project-b.yml`):
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: http://product-service:9001
          predicates:
            - Path=/api/products/**
        - id: inventory-service
          uri: http://inventory-service:9002
          predicates:
            - Path=/api/inventory/**
```

**Same gateway build** (`api-gateway:1.0.0`):
```bash
# Run with Project A config
java -jar api-gateway-1.0.0.jar \
  --spring.config.location=file:./config/application-project-a.yml

# Run with Project B config
java -jar api-gateway-1.0.0.jar \
  --spring.config.location=file:./config/application-project-b.yml
```

No source code changes required.

## Docker

### Building the Image

```bash
docker build -t api-gateway:1.0.0 .
```

### Docker Compose Examples

Example configurations for different projects are provided in the `config/` directory:

- `docker-compose-project-a.yml` - Example with user service
- `docker-compose-project-b.yml` - Example with product/inventory services

```bash
# Run with Project A configuration
docker-compose -f config/docker-compose-project-a.yml up

# Run with Project B configuration
docker-compose -f config/docker-compose-project-b.yml up
```

## Versioning

The gateway follows semantic versioning: `MAJOR.MINOR.PATCH`

```
api-gateway:1.0.0
api-gateway:1.1.0
api-gateway:1.1.1
```

### Using a Specific Version

```bash
# Docker
docker run api-gateway:1.0.0

# Docker Compose
image: api-gateway:1.0.0
```

### Upgrading

1. Test the new version with your project configuration
2. Update your deployment to use the new version tag
3. Existing deployments remain on their current version until explicitly updated

**Important:** Updating the gateway does NOT automatically update existing projects. Each project explicitly chooses which version to use.

## Deploying to a New Project

1. **Copy the gateway artifact** (Docker image or JAR)
2. **Create your project configuration**:
   ```bash
   mkdir -p infrastructure/gateway
   cp config/application-project-a.yml infrastructure/gateway/application.yml
   ```
3. **Modify routes** to match your services
4. **Set environment variables** for your service URLs
5. **Deploy** with your configuration

## Project Structure

```
api-gateway/
├── src/main/java/
│   ├── config/
│   │   ├── CorsConfig.java              # CORS configuration
│   │   └── RateLimitService.java        # Rate limiting service
│   ├── filter/
│   │   ├── AuthenticationFilter.java    # JWT authentication
│   │   ├── RateLimitGatewayFilter.java  # Rate limit filter
│   │   └── SecurityHeadersFilter.java   # Security headers
│   └── util/
│       └── JwtUtil.java                 # JWT utility
├── src/main/resources/
│   ├── application.properties           # Default properties
│   └── application.yml                  # Default YAML config
├── config/
│   ├── application-project-a.yml        # Example: Project A config
│   ├── application-project-b.yml        # Example: Project B config
│   ├── docker-compose-project-a.yml     # Example: Project A compose
│   └── docker-compose-project-b.yml     # Example: Project B compose
├── Dockerfile
├── Jenkinsfile
└── pom.xml
```

## Testing

```bash
# Run all tests
mvn test

# Run with specific profile
mvn test -Dspring.profiles.active=test
```

## Security

- JWT secrets must be provided via environment variables (never hardcoded)
- Rate limiting prevents brute force attacks
- Security headers protect against common vulnerabilities
- CORS restricts allowed origins
