package com.prateek.ProjectExpenseManagement.dto;

import java.time.Instant;
import java.util.UUID;

public class UserProfileResponse {

    private UUID userId;
    private String email;
    private String fullName;
    private String mobileNumber;
    private String role;
    private Instant createdAt;

    public UserProfileResponse(UUID userId, String email, String fullName, String mobileNumber,
                               String role, Instant createdAt) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
