package io.github.johneliud.api_gateway.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
        ReflectionTestUtils.setField(rateLimitService, "capacity", 5);
        ReflectionTestUtils.setField(rateLimitService, "refillTokens", 5);
        ReflectionTestUtils.setField(rateLimitService, "refillMinutes", 15);
    }

    @Test
    void tryConsume_withinLimit_returnsTrue() {
        assertTrue(rateLimitService.tryConsume("ip-1"));
    }

    @Test
    void tryConsume_exceedsLimit_returnsFalse() {
        for (int i = 0; i < 5; i++) {
            rateLimitService.tryConsume("ip-2");
        }
        assertFalse(rateLimitService.tryConsume("ip-2"));
    }

    @Test
    void tryConsume_differentKeys_independentBuckets() {
        assertTrue(rateLimitService.tryConsume("ip-3"));
        assertTrue(rateLimitService.tryConsume("ip-4"));
    }
}
