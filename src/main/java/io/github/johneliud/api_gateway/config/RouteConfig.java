package io.github.johneliud.api_gateway.config;

import io.github.johneliud.api_gateway.filter.AuthenticationFilter;
import io.github.johneliud.api_gateway.filter.RateLimitGatewayFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Value("${user.microservice.url}")
    private String userMicroserviceUrl;

    @Value("${movie.service.url}")
    private String movieServiceUrl;

    @Value("${rating.service.url}")
    private String ratingServiceUrl;

    @Value("${recommendation.service.url}")
    private String recommendationServiceUrl;

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder,
                                      AuthenticationFilter authFilter,
                                      RateLimitGatewayFilter rateLimitFilter) {
        return builder.routes()

                // ── User Microservice — public auth endpoints ──────────────────────
                .route("auth-register", r -> r.path("/api/auth/register").and().method("POST")
                        .uri(userMicroserviceUrl))

                .route("auth-login", r -> r.path("/api/auth/login").and().method("POST")
                        .filters(f -> f.filter(rateLimitFilter.apply(new RateLimitGatewayFilter.Config())))
                        .uri(userMicroserviceUrl))

                .route("auth-refresh", r -> r.path("/api/auth/refresh").and().method("POST")
                        .uri(userMicroserviceUrl))

                .route("auth-logout", r -> r.path("/api/auth/logout").and().method("POST")
                        .uri(userMicroserviceUrl))

                // User Microservice — public 2FA step (no full JWT yet, only MFA token)
                .route("auth-2fa-authenticate", r -> r.path("/api/auth/2fa/authenticate").and().method("POST")
                        .uri(userMicroserviceUrl))

                // User Microservice — authenticated 2FA management (specific before catch-all)
                .route("auth-2fa", r -> r.path("/api/auth/2fa/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(userMicroserviceUrl))

                .route("user-profile", r -> r.path("/api/users/profile/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(userMicroserviceUrl))

                .route("user-admin", r -> r.path("/api/users/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(userMicroserviceUrl))

                // ── Movie Service — public read endpoints ──────────────────────────
                .route("movie-search", r -> r.path("/api/movies/search").and().method("GET")
                        .uri(movieServiceUrl))

                .route("movie-list", r -> r.path("/api/movies").and().method("GET")
                        .uri(movieServiceUrl))

                .route("movie-by-id", r -> r.path("/api/movies/{id}").and().method("GET")
                        .uri(movieServiceUrl))

                // Movie Service — authenticated write endpoints (specific before catch-all)
                .route("movie-auth", r -> r.path("/api/movies/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(movieServiceUrl))

                // ── Rating Service — public read endpoints ─────────────────────────
                .route("ratings-by-movie", r -> r.path("/api/ratings/movie/{movieId}").and().method("GET")
                        .uri(ratingServiceUrl))

                .route("ratings-by-user", r -> r.path("/api/ratings/user/{userId}").and().method("GET")
                        .uri(ratingServiceUrl))

                // Rating Service — authenticated write endpoints
                .route("ratings-auth", r -> r.path("/api/ratings/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(ratingServiceUrl))

                // ── Recommendation Service — all authenticated ─────────────────────
                .route("recommendations", r -> r.path("/api/recommendations/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(recommendationServiceUrl))

                .build();
    }
}
