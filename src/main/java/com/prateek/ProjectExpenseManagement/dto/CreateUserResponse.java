package com.prateek.ProjectExpenseManagement.dto;

import java.util.UUID;

public class CreateUserResponse {
    private UUID userId;
    private String status;
    private String message;

    public CreateUserResponse(UUID userId, String status, String message) {
        this.userId = userId;
        this.status = status;
        this.message = message;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
