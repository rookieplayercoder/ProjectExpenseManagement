package com.prateek.ProjectExpenseManagement.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public class AddGroupMembersRequest {

    @NotEmpty
    private List<UUID> userIds;

    public List<UUID> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<UUID> userIds) {
        this.userIds = userIds;
    }
}
