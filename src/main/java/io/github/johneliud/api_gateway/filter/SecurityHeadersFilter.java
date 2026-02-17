package io.github.johneliud.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter extends AbstractGatewayFilterFactory<SecurityHeadersFilter.Config> {

    public SecurityHeadersFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
                exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
                exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
                exchange.getResponse().getHeaders().add("Content-Security-Policy", "default-src 'self'");
            }));
        };
    }

    public static class Config {
    }
}
