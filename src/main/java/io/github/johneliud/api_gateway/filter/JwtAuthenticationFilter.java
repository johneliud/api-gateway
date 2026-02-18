package io.github.johneliud.api_gateway.filter;

import io.github.johneliud.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            }

            try {
                String token = authHeader.substring(7);
                Claims claims = jwtUtil.validateToken(token);
                String userId = jwtUtil.getUserId(claims);
                String role = jwtUtil.getRole(claims);

                exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header("X-User-Id", userId)
                                .header("X-User-Role", role)
                                .build())
                        .build());
            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }
        };
    }

    public static class Config {
    }
}
