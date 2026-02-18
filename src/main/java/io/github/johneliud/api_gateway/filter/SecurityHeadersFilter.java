package io.github.johneliud.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityHeadersFilter extends AbstractGatewayFilterFactory<SecurityHeadersFilter.Config> {

    public SecurityHeadersFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> chain.filter(exchange).then(
            reactor.core.publisher.Mono.fromRunnable(() -> {
                exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
                exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
                exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
            })
        );
    }

    public static class Config {
    }
}
