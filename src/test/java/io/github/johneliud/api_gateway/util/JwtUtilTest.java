package io.github.johneliud.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String TEST_SECRET = "NqnGzaDEIZhGXWnbnWDHViZyKhinshBQ";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
    }

    private String buildToken(String userId, String role, Date expiry) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    @Test
    void validateToken_validToken_returnsClaims() {
        String token = buildToken("user123", "ROLE_USER", new Date(System.currentTimeMillis() + 3_600_000));
        Claims claims = jwtUtil.validateToken(token);
        assertNotNull(claims);
        assertEquals("user123", claims.getSubject());
    }

    @Test
    void getUserId_returnsSubject() {
        String token = buildToken("user123", "ROLE_USER", new Date(System.currentTimeMillis() + 3_600_000));
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("user123", jwtUtil.getUserId(claims));
    }

    @Test
    void getRole_returnsRoleClaim() {
        String token = buildToken("user123", "ROLE_ADMIN", new Date(System.currentTimeMillis() + 3_600_000));
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("ROLE_ADMIN", jwtUtil.getRole(claims));
    }

    @Test
    void validateToken_expiredToken_throwsException() {
        String token = buildToken("user123", "ROLE_USER", new Date(System.currentTimeMillis() - 1_000));
        assertThrows(Exception.class, () -> jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_throwsException() {
        assertThrows(Exception.class, () -> jwtUtil.validateToken("not.a.valid.token"));
    }
}
