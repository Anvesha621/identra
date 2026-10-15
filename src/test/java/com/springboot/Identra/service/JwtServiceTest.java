package com.springboot.Identra.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String testSecret = "VGhpc0lzQVNlY3VyZVRlc3RLZXlGb3JKV1RTZXJ2aWNlMjAyNg==";

        jwtService = new JwtService(testSecret, 900000);
    }

    @Test
    void generateTokenShouldContainCorrectUsername() {

        String token = jwtService.generateToken("testuser");

        assertNotNull(token);

        String username = jwtService.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    void generateTokenShouldProduceValidSignedToken() {

        String token = jwtService.generateToken("testuser");

        assertNotNull(token);
        assertEquals(2, token.chars().filter(ch -> ch == '.').count());
    }

    @Test
    void extractUsernameShouldRejectTamperedToken() {

        String token = jwtService.generateToken("testuser");

        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        assertThrows(
                Exception.class,
                () -> jwtService.extractUsername(tamperedToken)
        );
    }

    @Test
    void extractUsernameShouldRejectMalformedToken() {

        String malformedToken = "this.is.not-a-valid-jwt";

        assertThrows(
                Exception.class,
                () -> jwtService.extractUsername(malformedToken)
        );
    }

    @Test
    void extractUsernameShouldRejectExpiredToken() {

        SecretKey key = Keys.hmacShaKeyFor(
                java.util.Base64.getDecoder().decode(
                        "VGhpc0lzQVNlY3VyZVRlc3RLZXlGb3JKV1RTZXJ2aWNlMjAyNg=="
                )
        );

        Date now = new Date();
        Date expiredTime = new Date(now.getTime() - 60_000);

        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date(now.getTime() - 120_000))
                .expiration(expiredTime)
                .signWith(key)
                .compact();

        assertThrows(
                Exception.class,
                () -> jwtService.extractUsername(expiredToken)
        );
    }
}
