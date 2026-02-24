package io.github.johneliud.api_gateway.config;

import io.github.johneliud.api_gateway.handler.ProxyHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouteConfig {

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${media.service.url}")
    private String mediaServiceUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes(ProxyHandler handler) {
        return route(POST("/api/users/register"), 
                    req -> handler.proxyRequest(req, userServiceUrl, false, false))
                
                .andRoute(POST("/api/users/login"), 
                    req -> handler.proxyRequest(req, userServiceUrl, false, true))
                
                .andRoute(GET("/api/users/avatars/{filename}"), 
                    req -> handler.proxyRequest(req, userServiceUrl, false, false))
                
                .andRoute(path("/api/users/profile/**"), 
                    req -> handler.proxyRequest(req, userServiceUrl, true, false))
                
                .andRoute(GET("/api/products/my-products"), 
                    req -> handler.proxyRequest(req, productServiceUrl, true, false))
                
                .andRoute(GET("/api/products").or(GET("/api/products/{id}")), 
                    req -> handler.proxyRequest(req, productServiceUrl, false, false))
                
                .andRoute(path("/api/products/**"), 
                    req -> handler.proxyRequest(req, productServiceUrl, true, false))
                
                .andRoute(GET("/api/media/{id}"), 
                    req -> handler.proxyRequest(req, mediaServiceUrl, false, false))
                
                .andRoute(GET("/api/media/product/{productId}"), 
                    req -> handler.proxyRequest(req, mediaServiceUrl, false, false))
                
                .andRoute(path("/api/media/**"), 
                    req -> handler.proxyRequest(req, mediaServiceUrl, true, false));
    }
}