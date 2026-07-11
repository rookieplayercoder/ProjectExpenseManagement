package com.prateek.ProjectExpenseManagement.dto;

import java.time.Instant;

public class ApiErrorResponse {
    private final String error;
    private final String message;
    private final Instant timestamp;

    public ApiErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
