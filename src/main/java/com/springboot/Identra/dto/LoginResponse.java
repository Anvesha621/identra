package com.springboot.Identra.dto;

public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;

    public LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType) {

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }
}
