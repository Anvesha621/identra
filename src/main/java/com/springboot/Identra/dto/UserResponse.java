package com.springboot.Identra.dto;

public class UserResponse {

    private long id;
    private String username;
    private String email;
    private String role;
    private Boolean enabled;

    public UserResponse(long id, String username, String email, String role, Boolean enabled) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Boolean getEnabled() {
        return enabled;
    }
}
