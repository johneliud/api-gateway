package io.github.johneliud.api_gateway.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

    private static final String TEST_SECRET = "ThisIsATestSecretThisIsATestSecret";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
    }

    private String buildToken(String userId, List<String> roles, Date expiry) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("roles", roles)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    @Test
    void validateToken_validToken_returnsClaims() {
        String token = buildToken("user123", List.of("CLIENT"), new Date(System.currentTimeMillis() + 3_600_000));
        Claims claims = jwtUtil.validateToken(token);
        assertNotNull(claims);
        assertEquals("user123", claims.getSubject());
    }

    @Test
    void getUserId_returnsSubject() {
        String token = buildToken("user123", List.of("CLIENT"), new Date(System.currentTimeMillis() + 3_600_000));
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("user123", jwtUtil.getUserId(claims));
    }

    @Test
    void getRoles_returnsRolesClaim() {
        String token = buildToken("user123", List.of("ADMIN", "TRAVELER"), new Date(System.currentTimeMillis() + 3_600_000));
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("ADMIN,TRAVELER", jwtUtil.getRoles(claims));
    }

    @Test
    void getRoles_singleRole_returnsSingleValue() {
        String token = buildToken("user123", List.of("SELLER"), new Date(System.currentTimeMillis() + 3_600_000));
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("SELLER", jwtUtil.getRoles(claims));
    }

    @Test
    void validateToken_expiredToken_throwsException() {
        String token = buildToken("user123", List.of("CLIENT"), new Date(System.currentTimeMillis() - 1_000));
        assertThrows(Exception.class, () -> jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_throwsException() {
        assertThrows(Exception.class, () -> jwtUtil.validateToken("not.a.valid.token"));
    }
}
