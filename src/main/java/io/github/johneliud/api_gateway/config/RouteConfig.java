package io.github.johneliud.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.johneliud.api_gateway.filter.AuthenticationFilter;
import io.github.johneliud.api_gateway.filter.RateLimitGatewayFilter;

@Configuration
public class RouteConfig {

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder,
                                      AuthenticationFilter authFilter,
                                      RateLimitGatewayFilter rateLimitFilter) {
        return builder.routes()
                // Public user routes
                .route("user-register", r -> r.path("/api/users/register").and().method("POST")
                        .uri(userServiceUrl))

                .route("user-login", r -> r.path("/api/users/login").and().method("POST")
                        .filters(f -> f.filter(rateLimitFilter.apply(new RateLimitGatewayFilter.Config())))
                        .uri(userServiceUrl))

                .route("user-avatar", r -> r.path("/api/users/avatars/{filename}").and().method("GET")
                        .uri(userServiceUrl))

                .route("user-by-id", r -> r.path("/api/users/{id}").and().method("GET")
                        .uri(userServiceUrl))

                // Authenticated user routes
                .route("user-profile", r -> r.path("/api/users/profile/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(userServiceUrl))

                .build();
    }
}
