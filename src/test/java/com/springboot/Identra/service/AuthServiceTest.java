package com.springboot.Identra.service;

import com.springboot.Identra.dto.LoginRequest;
import com.springboot.Identra.dto.RegisterRequest;
import com.springboot.Identra.entity.User;
import com.springboot.Identra.exception.AccountLockedException;
import com.springboot.Identra.exception.DuplicateResourceException;
import com.springboot.Identra.exception.InvalidCredentialsException;
import com.springboot.Identra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
    }

    @Test
    void registerShouldSaveUserWithEncodedPassword() {

        when(userRepository.existsByUsername("testuser"))
                .thenReturn(false);

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password123!"))
                .thenReturn("$2a$12$encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = authService.register(request);

        assertNotNull(savedUser);
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("$2a$12$encodedPassword", savedUser.getPassword());
        assertEquals("USER", savedUser.getRole());

        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldThrowExceptionWhenUsernameAlreadyExists() {

        when(userRepository.existsByUsername("testuser"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldThrowExceptionWhenEmailAlreadyExists() {

        when(userRepository.existsByUsername("testuser"))
                .thenReturn(false);

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginShouldThrowExceptionForWrongPassword() {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("WrongPassword123");

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("$2a$12$encodedPassword");
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);

        when(userRepository.findByUsernameForUpdate("testuser"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword123",
                "$2a$12$encodedPassword"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(1, user.getFailedLoginAttempts());

        verify(userRepository).save(user);
    }

    @Test
    void loginShouldLockAccountAfterFiveFailedAttempts() {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("WrongPassword123");

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("$2a$12$encodedPassword");
        user.setEnabled(true);
        user.setFailedLoginAttempts(4);

        when(userRepository.findByUsernameForUpdate("testuser"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword123",
                "$2a$12$encodedPassword"
        )).thenReturn(false);

        assertThrows(
                AccountLockedException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(5, user.getFailedLoginAttempts());

        assertNotNull(user.getLockedUntil());

        assertTrue(
                user.getLockedUntil().isAfter(LocalDateTime.now())
        );

        verify(userRepository).save(user);
    }

    @Test
    void loginShouldRejectAccountThatIsAlreadyLocked() {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("CorrectPassword123");

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("$2a$12$encodedPassword");
        user.setEnabled(true);
        user.setFailedLoginAttempts(5);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByUsernameForUpdate("testuser"))
                .thenReturn(Optional.of(user));

        assertThrows(
                AccountLockedException.class,
                () -> authService.login(loginRequest)
        );

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(userRepository, never()).save(any(User.class));
    }
}
