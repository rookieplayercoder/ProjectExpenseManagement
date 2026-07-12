package com.prateek.ProjectExpenseManagement.dto;

import java.time.Instant;
import java.util.UUID;

public class GroupSummaryResponse {

    private UUID groupId;
    private String groupName;
    private String description;
    private UUID createdByUserId;
    private int memberCount;
    private Instant createdAt;

    public GroupSummaryResponse(UUID groupId, String groupName, String description,
                                UUID createdByUserId, int memberCount, Instant createdAt) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.description = description;
        this.createdByUserId = createdByUserId;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
