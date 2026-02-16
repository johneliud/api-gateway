package io.github.johneliud.api_gateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitService {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    @Value("${rate.limit.login.capacity:5}")
    private int capacity;
    
    @Value("${rate.limit.login.refill.tokens:5}")
    private int refillTokens;
    
    @Value("${rate.limit.login.refill.minutes:15}")
    private int refillMinutes;

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(refillTokens, Duration.ofMinutes(refillMinutes)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume(String key) {
        Bucket bucket = resolveBucket(key);
        boolean consumed = bucket.tryConsume(1);
        if (!consumed) {
            log.warn("Rate limit exceeded for key: {}", key);
        }
        return consumed;
    }
}
