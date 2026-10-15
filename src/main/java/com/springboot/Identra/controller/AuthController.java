package com.springboot.Identra.controller;

import com.springboot.Identra.dto.*;
import com.springboot.Identra.entity.RefreshToken;
import com.springboot.Identra.entity.User;
import com.springboot.Identra.service.AuthService;
import com.springboot.Identra.service.JwtService;
import com.springboot.Identra.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {



    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {

        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        UserResponse response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getEnabled()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        User user = authService.login(request);

        String token = jwtService.generateToken(user.getUsername());

        String accessToken = jwtService.generateToken(user.getUsername());

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        RefreshToken newRefreshToken =
                refreshTokenService.rotateRefreshToken(
                        request.getRefreshToken()
                );

        String newAccessToken =
                jwtService.generateToken(
                        newRefreshToken.getUser().getUsername()
                );

        LoginResponse response = new LoginResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                "Bearer"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Valid @RequestBody RefreshRequest request) {

        refreshTokenService.revokeToken(
                request.getRefreshToken()
        );

        return ResponseEntity.ok("Logged out successfully");
    }
}
