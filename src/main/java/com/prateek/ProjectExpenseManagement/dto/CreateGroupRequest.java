package com.prateek.ProjectExpenseManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class CreateGroupRequest {

    @NotBlank
    @Size(max = 150)
    private String groupName;

    @Size(max = 2000)
    private String description;

    @NotNull
    private UUID createdByUserId;

    /**
     * Additional members to add alongside the creator. The creator is always
     * added as a member automatically and does not need to be repeated here.
     */
    private List<UUID> memberUserIds;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public List<UUID> getMemberUserIds() {
        return memberUserIds;
    }

    public void setMemberUserIds(List<UUID> memberUserIds) {
        this.memberUserIds = memberUserIds;
    }
}
