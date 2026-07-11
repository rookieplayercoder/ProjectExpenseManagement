package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.dto.SettlementSummaryResponse;
import com.prateek.ProjectExpenseManagement.repository.GroupRepository;
import com.prateek.ProjectExpenseManagement.repository.SettlementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SettlementQueryService {

    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;

    public SettlementQueryService(SettlementRepository settlementRepository, GroupRepository groupRepository) {
        this.settlementRepository = settlementRepository;
        this.groupRepository = groupRepository;
    }

    public List<SettlementSummaryResponse> getSettlementsForGroup(UUID groupId) {
        groupRepository.assertGroupExists(groupId);
        return settlementRepository.findByGroupId(groupId);
    }
}
