package io.github.johneliud.api_gateway.filter;

import io.github.johneliud.api_gateway.config.RateLimitService;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class RateLimitGatewayFilter extends AbstractGatewayFilterFactory<RateLimitGatewayFilter.Config> {

    private final RateLimitService rateLimitService;

    public RateLimitGatewayFilter(RateLimitService rateLimitService) {
        super(Config.class);
        this.rateLimitService = rateLimitService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientIp = getClientIp(exchange);
            if (!rateLimitService.tryConsume(clientIp)) {
                return onRateLimitExceeded(exchange);
            }
            return chain.filter(exchange);
        };
    }

    private String getClientIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"Rate limit exceeded\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {}
}
