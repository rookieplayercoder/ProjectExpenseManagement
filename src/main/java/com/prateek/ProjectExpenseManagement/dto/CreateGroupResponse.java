package com.prateek.ProjectExpenseManagement.dto;

import java.util.UUID;

public class CreateGroupResponse {
    private UUID groupId;
    private String status;
    private String message;

    public CreateGroupResponse(UUID groupId, String status, String message) {
        this.groupId = groupId;
        this.status = status;
        this.message = message;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
