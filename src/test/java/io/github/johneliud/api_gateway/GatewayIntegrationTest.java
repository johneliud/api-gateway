package io.github.johneliud.api_gateway;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

    private static final String TEST_SECRET = "ThisIsATestSecretThisIsATestSecret";

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private String validToken(String userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    @Test
    void protectedRoute_noAuthHeader_returns401() {
        webTestClient.get().uri("/api/users/profile/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedRoute_invalidToken_returns401() {
        webTestClient.get().uri("/api/users/profile/me")
                .header("Authorization", "Bearer invalid.token.here")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedRoute_malformedAuthHeader_returns401() {
        webTestClient.get().uri("/api/users/profile/me")
                .header("Authorization", "Token " + validToken("user1", "CLIENT"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void anyResponse_hasSecurityHeaders() {
        webTestClient.get().uri("/api/users/profile/me")
                .exchange()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().valueEquals("X-XSS-Protection", "1; mode=block")
                .expectHeader().exists("Content-Security-Policy");
    }
}
