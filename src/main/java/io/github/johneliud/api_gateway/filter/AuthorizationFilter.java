package io.github.johneliud.api_gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationFilter.class);
    private static final String ROLE_PREFIX = "ROLE_";

    public AuthorizationFilter() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("requiredRole");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String requiredRole = config.requiredRole;

            if (requiredRole == null || requiredRole.isBlank()) {
                return chain.filter(exchange);
            }

            String userRolesHeader = exchange.getRequest().getHeaders()
                    .getFirst("X-User-Roles");

            if (userRolesHeader == null || userRolesHeader.isBlank()) {
                log.error("Missing X-User-Roles header for role-protected route");
                return onError(exchange, "Forbidden: insufficient permissions",
                        HttpStatus.FORBIDDEN);
            }

            List<String> userRoles = Arrays.stream(userRolesHeader.split(","))
                    .map(String::trim)
                    .map(role -> ROLE_PREFIX + role)
                    .toList();

            if (!userRoles.contains(ROLE_PREFIX + requiredRole)) {
                log.error("User lacks required role: {}", requiredRole);
                return onError(exchange, "Forbidden: insufficient permissions",
                        HttpStatus.FORBIDDEN);
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {
        private String requiredRole;

        public String getRequiredRole() {
            return requiredRole;
        }

        public void setRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
        }
    }
}
