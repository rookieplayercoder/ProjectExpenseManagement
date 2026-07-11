package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.dto.GroupBalanceResponse;
import com.prateek.ProjectExpenseManagement.repository.BalanceRepository;
import com.prateek.ProjectExpenseManagement.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final GroupRepository groupRepository;

    public BalanceService(BalanceRepository balanceRepository, GroupRepository groupRepository) {
        this.balanceRepository = balanceRepository;
        this.groupRepository = groupRepository;
    }

    public GroupBalanceResponse getGroupBalances(UUID groupId) {
        groupRepository.assertGroupExists(groupId);
        return new GroupBalanceResponse(groupId, balanceRepository.findBalancesForGroup(groupId));
    }
}
