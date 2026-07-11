package com.prateek.ProjectExpenseManagement.dto;

import java.util.UUID;

public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private UUID userId;
    private String email;
    private String role;

    public LoginResponse(String accessToken, UUID userId, String email, String role) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
