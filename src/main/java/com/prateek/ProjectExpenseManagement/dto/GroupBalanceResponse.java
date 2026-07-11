package com.prateek.ProjectExpenseManagement.dto;

import java.util.List;
import java.util.UUID;

public class GroupBalanceResponse {

    private UUID groupId;
    private List<BalanceEntryResponse> balances;

    public GroupBalanceResponse(UUID groupId, List<BalanceEntryResponse> balances) {
        this.groupId = groupId;
        this.balances = balances;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public List<BalanceEntryResponse> getBalances() {
        return balances;
    }
}
