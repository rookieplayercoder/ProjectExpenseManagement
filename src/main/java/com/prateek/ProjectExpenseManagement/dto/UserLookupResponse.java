package com.prateek.ProjectExpenseManagement.dto;

import java.util.UUID;

public class UserLookupResponse {

    private UUID userId;
    private String fullName;
    private String email;

    public UserLookupResponse(UUID userId, String fullName, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }
}
