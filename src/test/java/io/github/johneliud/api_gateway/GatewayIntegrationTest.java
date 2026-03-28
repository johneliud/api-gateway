package io.github.johneliud.api_gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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
        webTestClient.get().uri("/api/users/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedRoute_invalidToken_returns401() {
        webTestClient.get().uri("/api/users/profile")
                .header("Authorization", "Bearer invalid.token.here")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedRoute_malformedAuthHeader_returns401() {
        webTestClient.get().uri("/api/users/profile")
                .header("Authorization", "Token " + validToken("user1", "ROLE_USER"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void anyResponse_hasSecurityHeaders() {
        webTestClient.get().uri("/api/users/profile")
                .exchange()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().valueEquals("X-XSS-Protection", "1; mode=block")
                .expectHeader().exists("Content-Security-Policy");
    }

    @Test
    void movieList_noAuth_isAllowed() {
        webTestClient.get().uri("/api/movies")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    void movieCreate_noAuth_returns401() {
        webTestClient.post().uri("/api/movies")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void movieUpdate_noAuth_returns401() {
        webTestClient.put().uri("/api/movies/some-id")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void movieDelete_noAuth_returns401() {
        webTestClient.delete().uri("/api/movies/some-id")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void ratingsByMovie_noAuth_isAllowed() {
        webTestClient.get().uri("/api/ratings/movie/some-id")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    void ratingCreate_noAuth_returns401() {
        webTestClient.post().uri("/api/ratings")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void recommendations_noAuth_returns401() {
        webTestClient.get().uri("/api/recommendations")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void userMe_noAuth_returns401() {
        webTestClient.get().uri("/api/users/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void userWatchlist_noAuth_returns401() {
        webTestClient.get().uri("/api/users/profile/watchlist")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void twoFaSetup_noAuth_returns401() {
        webTestClient.post().uri("/api/auth/2fa/setup")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}