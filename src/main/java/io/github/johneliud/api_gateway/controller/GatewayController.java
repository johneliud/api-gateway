package io.github.johneliud.api_gateway.controller;

import io.github.johneliud.api_gateway.config.RateLimitService;
import io.github.johneliud.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GatewayController {

    private final WebClient.Builder webClientBuilder;
    private final JwtUtil jwtUtil;
    private final RateLimitService rateLimitService;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${media.service.url}")
    private String mediaServiceUrl;

    // User Service Routes
    @RequestMapping(value = "/api/users/register", method = RequestMethod.POST)
    public Mono<String> userRegister(ServerHttpRequest request, @RequestBody String body) {
        return proxy(userServiceUrl + "/api/users/register", request, body, false, false);
    }

    @RequestMapping(value = "/api/users/login", method = RequestMethod.POST)
    public Mono<String> userLogin(ServerHttpRequest request, @RequestBody String body) {
        String clientIp = getClientIp(request);
        if (!rateLimitService.tryConsume(clientIp)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
        return proxy(userServiceUrl + "/api/users/login", request, body, false, false);
    }

    @RequestMapping(value = "/api/users/profile/**", method = {RequestMethod.GET, RequestMethod.PUT})
    public Mono<String> userProfile(ServerHttpRequest request, @RequestBody(required = false) String body) {
        return proxy(userServiceUrl + request.getPath().value(), request, body, true, false);
    }

    // Product Service Routes
    @RequestMapping(value = "/api/products", method = RequestMethod.GET)
    public Mono<String> getProducts(ServerHttpRequest request) {
        return proxy(productServiceUrl + "/api/products" + getQueryString(request), request, null, false, false);
    }

    @RequestMapping(value = "/api/products/{id}", method = RequestMethod.GET)
    public Mono<String> getProduct(ServerHttpRequest request, @PathVariable String id) {
        return proxy(productServiceUrl + "/api/products/" + id, request, null, false, false);
    }

    @RequestMapping(value = "/api/products/**", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public Mono<String> productOperations(ServerHttpRequest request, @RequestBody(required = false) String body) {
        return proxy(productServiceUrl + request.getPath().value() + getQueryString(request), request, body, true, false);
    }

    // Media Service Routes
    @RequestMapping(value = "/api/media/{id}", method = RequestMethod.GET)
    public Mono<String> getMedia(ServerHttpRequest request, @PathVariable String id) {
        return proxy(mediaServiceUrl + "/api/media/" + id, request, null, false, false);
    }

    @RequestMapping(value = "/api/media/product/{productId}", method = RequestMethod.GET)
    public Mono<String> getMediaByProduct(ServerHttpRequest request, @PathVariable String productId) {
        return proxy(mediaServiceUrl + "/api/media/product/" + productId, request, null, false, false);
    }

    @RequestMapping(value = "/api/media/**", method = {RequestMethod.POST, RequestMethod.DELETE})
    public Mono<String> mediaOperations(ServerHttpRequest request, @RequestBody(required = false) String body) {
        return proxy(mediaServiceUrl + request.getPath().value() + getQueryString(request), request, body, true, false);
    }

    private Mono<String> proxy(String targetUrl, ServerHttpRequest request, String body, boolean requiresAuth, boolean isMultipart) {
        WebClient.RequestBodySpec requestSpec = webClientBuilder.build()
                .method(request.getMethod())
                .uri(targetUrl)
                .headers(headers -> request.getHeaders().forEach((key, values) -> {
                    if (!key.equalsIgnoreCase("host")) {
                        headers.addAll(key, values);
                    }
                }));

        if (requiresAuth) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            }
            try {
                String token = authHeader.substring(7);
                Claims claims = jwtUtil.validateToken(token);
                String userId = jwtUtil.getUserId(claims);
                String role = jwtUtil.getRole(claims);
                
                requestSpec.header("X-User-Id", userId);
                requestSpec.header("X-User-Role", role);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }
        }

        if (body != null) {
            requestSpec.bodyValue(body);
        }

        return requestSpec.retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("Proxy error: {}", e.getMessage()));
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private String getQueryString(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        return query != null ? "?" + query : "";
    }
}
