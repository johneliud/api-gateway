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

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${media.service.url}")
    private String mediaServiceUrl;

    @Value("${order.service.url}")
    private String orderServiceUrl;

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

                // Product routes — specific before catch-all
                .route("product-my-products", r -> r.path("/api/products/my-products").and().method("GET")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(productServiceUrl))

                .route("product-list", r -> r.path("/api/products").and().method("GET")
                        .uri(productServiceUrl))

                .route("product-by-id", r -> r.path("/api/products/{id}").and().method("GET")
                        .uri(productServiceUrl))

                .route("product-auth", r -> r.path("/api/products/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(productServiceUrl))

                // Media routes — specific before catch-all
                .route("media-by-id", r -> r.path("/api/media/{id}").and().method("GET")
                        .uri(mediaServiceUrl))

                .route("media-by-product", r -> r.path("/api/media/product/{productId}").and().method("GET")
                        .uri(mediaServiceUrl))

                .route("media-auth", r -> r.path("/api/media/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(mediaServiceUrl))

                // Order and cart routes (all authenticated)
                .route("orders", r -> r.path("/api/orders/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(orderServiceUrl))

                .route("cart", r -> r.path("/api/cart/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri(orderServiceUrl))

                .build();
    }
}
