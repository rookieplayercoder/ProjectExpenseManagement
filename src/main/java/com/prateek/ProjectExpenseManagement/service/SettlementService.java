package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.dto.SettleBalanceRequest;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import com.prateek.ProjectExpenseManagement.repository.BalanceRepository;
import com.prateek.ProjectExpenseManagement.repository.GroupRepository;
import com.prateek.ProjectExpenseManagement.repository.IdempotencyRepository;
import com.prateek.ProjectExpenseManagement.repository.SettlementRepository;
import com.prateek.ProjectExpenseManagement.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SettlementService {

    private static final String REQUEST_TYPE = "SETTLE_BALANCE";

    private final SettlementRepository settlementRepository;
    private final BalanceRepository balanceRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final IdempotencyRepository idempotencyRepository;

    public SettlementService(SettlementRepository settlementRepository,
                             BalanceRepository balanceRepository,
                             UserRepository userRepository,
                             GroupRepository groupRepository,
                             IdempotencyRepository idempotencyRepository) {
        this.settlementRepository = settlementRepository;
        this.balanceRepository = balanceRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UUID settleBalance(@Valid SettleBalanceRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            boolean claimed = idempotencyRepository.reserveKey(idempotencyKey, REQUEST_TYPE);
            if (!claimed) {
                UUID existingSettlementId = idempotencyRepository.findReferenceId(idempotencyKey, REQUEST_TYPE);
                if (existingSettlementId == null) {
                    throw new BusinessValidationException(
                            "Idempotency key is currently being processed by another request. Please retry.");
                }
                return existingSettlementId;
            }
        }

        String currencyCode = request.getCurrencyCode().toUpperCase();

        List<UUID> userIds = List.of(
                request.getPaidByUserId(),
                request.getPaidToUserId(),
                request.getCreatedByUserId()
        );

        userRepository.assertUsersExist(userIds);
        groupRepository.assertGroupExists(request.getGroupId());
        groupRepository.assertUsersBelongToGroup(request.getGroupId(), userIds);

        UUID settlementId = settlementRepository.insertSettlement(request);

        // settlement means paidBy reduces debt toward paidTo
        balanceRepository.applyDebt(
                request.getGroupId(),
                request.getPaidToUserId(),
                request.getPaidByUserId(),
                currencyCode,
                request.getAmount(),
                "SETTLEMENT",
                settlementId
        );

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyRepository.attachReferenceId(idempotencyKey, REQUEST_TYPE, settlementId);
        }

        return settlementId;
    }
}
