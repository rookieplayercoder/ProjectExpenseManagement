package com.prateek.ProjectExpenseManagement.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GroupDetailResponse {

    private UUID groupId;
    private String groupName;
    private String description;
    private UUID createdByUserId;
    private Instant createdAt;
    private List<GroupMemberResponse> members;

    public GroupDetailResponse(UUID groupId, String groupName, String description,
                               UUID createdByUserId, Instant createdAt, List<GroupMemberResponse> members) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.description = description;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.members = members;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<GroupMemberResponse> getMembers() {
        return members;
    }
}
