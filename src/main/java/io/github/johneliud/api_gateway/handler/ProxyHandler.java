package io.github.johneliud.api_gateway.handler;

import io.github.johneliud.api_gateway.config.RateLimitService;
import io.github.johneliud.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProxyHandler {

    private static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);

    private final WebClient.Builder webClientBuilder;
    private final JwtUtil jwtUtil;
    private final RateLimitService rateLimitService;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${media.service.url}")
    private String mediaServiceUrl;

    public ProxyHandler(WebClient.Builder webClientBuilder, JwtUtil jwtUtil, RateLimitService rateLimitService) {
        this.webClientBuilder = webClientBuilder;
        this.jwtUtil = jwtUtil;
        this.rateLimitService = rateLimitService;
    }

    public Mono<ServerResponse> proxyRequest(ServerRequest request, String targetUrl, boolean requiresAuth, boolean requiresRateLimit) {
        log.info("Proxying {} {} to {}", request.method(), request.path(), targetUrl);

        if (requiresRateLimit) {
            String clientIp = getClientIp(request);
            if (!rateLimitService.tryConsume(clientIp)) {
                return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .bodyValue("{\"error\":\"Rate limit exceeded\"}");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        request.headers().asHttpHeaders().forEach((key, values) -> {
            if (!key.equalsIgnoreCase("Host") && !key.equalsIgnoreCase("Content-Length")) {
                headers.addAll(key, values);
            }
        });

        if (requiresAuth) {
            String authHeader = request.headers().firstHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                        .bodyValue("{\"error\":\"Missing or invalid Authorization header\"}");
            }

            try {
                String token = authHeader.substring(7);
                Claims claims = jwtUtil.validateToken(token);
                headers.set("X-User-Id", jwtUtil.getUserId(claims));
                headers.set("X-User-Role", jwtUtil.getRole(claims));
            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                        .bodyValue("{\"error\":\"Invalid or expired token\"}");
            }
        }

        WebClient webClient = webClientBuilder.build();
        
        return webClient.method(request.method())
                .uri(targetUrl + request.path())
                .headers(h -> h.addAll(headers))
                .body(BodyInserters.fromDataBuffers(request.bodyToFlux(DataBuffer.class)))
                .exchangeToMono(response -> {
                    ServerResponse.BodyBuilder responseBuilder = ServerResponse.status(response.statusCode());
                    
                    response.headers().asHttpHeaders().forEach((key, values) -> {
                        if (!key.equalsIgnoreCase("Transfer-Encoding")) {
                            responseBuilder.header(key, values.toArray(new String[0]));
                        }
                    });
                    
                    String contentType = response.headers().contentType()
                            .map(MediaType::toString)
                            .orElse("application/octet-stream");
                    
                    if (contentType.startsWith("image/")) {
                        return response.bodyToMono(byte[].class)
                                .flatMap(body -> responseBuilder.bodyValue(body));
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(body -> responseBuilder.bodyValue(body))
                                .switchIfEmpty(responseBuilder.build());
                    }
                })
                .onErrorResume(e -> {
                    log.error("Proxy error: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.BAD_GATEWAY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue("{\"error\":\"Service unavailable\"}");
                });
    }

    private String getClientIp(ServerRequest request) {
        String xForwardedFor = request.headers().firstHeader("X-Forwarded-For");
        if (xForwardedFor != null) {
            return xForwardedFor.split(",")[0];
        }
        ServerHttpRequest nativeRequest = request.exchange().getRequest();
        return nativeRequest.getRemoteAddress() != null 
                ? nativeRequest.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }
}
