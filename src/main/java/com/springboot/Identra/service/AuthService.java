package com.springboot.Identra.service;

import com.springboot.Identra.dto.LoginRequest;
import com.springboot.Identra.dto.RegisterRequest;
import com.springboot.Identra.entity.User;
import com.springboot.Identra.exception.AccountLockedException;
import com.springboot.Identra.exception.DuplicateResourceException;
import com.springboot.Identra.exception.InvalidCredentialsException;
import com.springboot.Identra.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setEnabled(true);

        return userRepository.save(user);
    }

    public User login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid username or password"
                        ));

        // Check whether the account is enabled
        if (!user.getEnabled()) {
            throw new InvalidCredentialsException(
                    "Invalid username or password"
            );
        }

        // Check whether the account is locked
        if (user.getLockedUntil() != null) {

            if (user.getLockedUntil().isAfter(
                    java.time.LocalDateTime.now())) {

                throw new AccountLockedException(
                        "Account is temporarily locked. Please try again later."
                );
            }

            // Lock has expired: reset failed attempts
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }

        // Verify password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            int attempts = user.getFailedLoginAttempts() + 1;

            user.setFailedLoginAttempts(attempts);

            if (attempts >= 5) {
                user.setLockedUntil(
                        java.time.LocalDateTime.now().plusMinutes(15)
                );
            }

            userRepository.save(user);

            // Return an account-locked error on the fifth failed attempt
            if (attempts >= 5) {
                throw new AccountLockedException(
                        "Too many failed login attempts. Account locked for 15 minutes."
                );
            }

            throw new InvalidCredentialsException(
                    "Invalid username or password"
            );
        }

        // Successful login: reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        return user;
    }
}
