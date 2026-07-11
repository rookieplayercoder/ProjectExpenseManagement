package com.prateek.ProjectExpenseManagement.dto;

import java.util.UUID;

public class CreateExpenseResponse {
    private UUID expenseId;
    private String status;
    private String message;

    public CreateExpenseResponse(UUID expenseId, String status, String message) {
        this.expenseId = expenseId;
        this.status = status;
        this.message = message;
    }

    public UUID getExpenseId() {
        return expenseId;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
