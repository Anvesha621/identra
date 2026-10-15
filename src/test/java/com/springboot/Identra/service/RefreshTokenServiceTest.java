package com.springboot.Identra.service;

import com.springboot.Identra.entity.RefreshToken;
import com.springboot.Identra.entity.User;
import com.springboot.Identra.repository.RefreshTokenRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
    }

    @Test
    void createRefreshTokenShouldSaveValidToken() {

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        assertNotNull(refreshToken.getToken());
        assertEquals(user, refreshToken.getUser());
        assertFalse(refreshToken.isRevoked());

        assertTrue(
                refreshToken.getExpiresAt().isAfter(LocalDateTime.now())
        );

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void verifyRefreshTokenShouldReturnValidToken() {

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-token");
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(refreshToken));

        RefreshToken result =
                refreshTokenService.verifyRefreshToken("valid-token");

        assertEquals("valid-token", result.getToken());
        assertFalse(result.isRevoked());
    }

    @Test
    void verifyRefreshTokenShouldRejectRevokedToken() {

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("revoked-token");
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked-token"))
                .thenReturn(Optional.of(refreshToken));

        assertThrows(
                RuntimeException.class,
                () -> refreshTokenService.verifyRefreshToken("revoked-token")
        );
    }

    @Test
    void verifyRefreshTokenShouldRejectExpiredToken() {

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("expired-token");
        refreshToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        refreshToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(refreshToken));

        assertThrows(
                RuntimeException.class,
                () -> refreshTokenService.verifyRefreshToken("expired-token")
        );
    }

    @Test
    void rotateRefreshTokenShouldRevokeOldTokenAndCreateNewOne() {

        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken("old-token");
        oldToken.setUser(user);
        oldToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        oldToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("old-token"))
                .thenReturn(Optional.of(oldToken));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken newToken =
                refreshTokenService.rotateRefreshToken("old-token");

        assertTrue(oldToken.isRevoked());

        assertNotNull(newToken.getToken());
        assertNotEquals("old-token", newToken.getToken());
        assertEquals(user, newToken.getUser());
        assertFalse(newToken.isRevoked());

        verify(refreshTokenRepository, times(2))
                .save(any(RefreshToken.class));
    }

    @Test
    void revokeTokenShouldMarkTokenAsRevoked() {

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("token-to-revoke");
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("token-to-revoke"))
                .thenReturn(Optional.of(refreshToken));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.revokeToken("token-to-revoke");

        assertTrue(refreshToken.isRevoked());

        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void rotateRefreshTokenShouldRejectReusingOldToken() {

        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken("old-token");
        oldToken.setUser(user);
        oldToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        oldToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("old-token"))
                .thenReturn(Optional.of(oldToken));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // First use: rotate the old token.
        RefreshToken newToken =
                refreshTokenService.rotateRefreshToken("old-token");

        assertNotNull(newToken);
        assertTrue(oldToken.isRevoked());

        // Second use: the old token should be rejected.
        assertThrows(
                RuntimeException.class,
                () -> refreshTokenService.rotateRefreshToken("old-token")
        );
    }
}
