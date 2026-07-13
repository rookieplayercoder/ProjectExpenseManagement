package com.prateek.ProjectExpenseManagement.dto;

import java.time.Instant;
import java.util.UUID;

public class GroupMemberResponse {

    private UUID userId;
    private String fullName;
    private String email;
    private Instant joinedAt;

    public GroupMemberResponse(UUID userId, String fullName, String email, Instant joinedAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.joinedAt = joinedAt;
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

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
