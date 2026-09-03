package io.github.johneliud.api_gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void gateway_startsSuccessfully() {
        // Verify the gateway is running and responds to health endpoint
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void anyResponse_hasSecurityHeaders() {
        // Security headers should be present on ALL responses
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().valueEquals("X-XSS-Protection", "1; mode=block")
                .expectHeader().exists("Content-Security-Policy");
    }

    @Test
    void requestWithoutRoute_returns404() {
        // Without any routes configured, requests should return 404
        webTestClient.get().uri("/api/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void healthEndpoint_returnsOk() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
