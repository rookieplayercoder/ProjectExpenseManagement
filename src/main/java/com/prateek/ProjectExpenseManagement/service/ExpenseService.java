package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.dto.CreateExpenseRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateExpenseResponse;
import com.prateek.ProjectExpenseManagement.dto.ExpenseDetailResponse;
import com.prateek.ProjectExpenseManagement.dto.ExpenseParticipantResponse;
import com.prateek.ProjectExpenseManagement.dto.UpdateExpenseRequest;
import com.prateek.ProjectExpenseManagement.dto.UpdateExpenseResponse;
import com.prateek.ProjectExpenseManagement.dto.ParticipantShareRequest;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import com.prateek.ProjectExpenseManagement.repository.BalanceRepository;
import com.prateek.ProjectExpenseManagement.repository.ExpenseRepository;
import com.prateek.ProjectExpenseManagement.repository.GroupRepository;
import com.prateek.ProjectExpenseManagement.repository.IdempotencyRepository;
import com.prateek.ProjectExpenseManagement.repository.UserRepository;
import com.prateek.ProjectExpenseManagement.strategy.ExpenseSplitStrategy;
import com.prateek.ProjectExpenseManagement.strategy.SplitStrategyFactory;
import com.prateek.ProjectExpenseManagement.domain.SplitAllocation;
import com.prateek.ProjectExpenseManagement.domain.SplitType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExpenseService {

    private static final String REQUEST_TYPE = "CREATE_EXPENSE";

    private final ExpenseRepository expenseRepository;
    private final BalanceRepository balanceRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final SplitStrategyFactory splitStrategyFactory;
    private final IdempotencyRepository idempotencyRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          BalanceRepository balanceRepository,
                          GroupRepository groupRepository,
                          UserRepository userRepository,
                          SplitStrategyFactory splitStrategyFactory,
                          IdempotencyRepository idempotencyRepository) {
        this.expenseRepository = expenseRepository;
        this.balanceRepository = balanceRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.splitStrategyFactory = splitStrategyFactory;
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public CreateExpenseResponse createExpense(@Valid CreateExpenseRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            boolean claimed = idempotencyRepository.reserveKey(idempotencyKey, REQUEST_TYPE);
            if (!claimed) {
                UUID existingExpenseId = idempotencyRepository.findReferenceId(idempotencyKey, REQUEST_TYPE);
                if (existingExpenseId == null) {
                    throw new BusinessValidationException(
                            "Idempotency key is currently being processed by another request. Please retry.");
                }
                return new CreateExpenseResponse(
                        existingExpenseId,
                        "SUCCESS",
                        "Duplicate request detected - returning result of original request"
                );
            }
        }

        String currencyCode = request.getCurrencyCode().toUpperCase();

        List<UUID> allUserIds = Stream.concat(
                Stream.of(request.getPaidByUserId(), request.getCreatedByUserId()),
                request.getParticipants().stream().map(p -> p.getUserId())
        ).distinct().collect(Collectors.toList());

        userRepository.assertUsersExist(allUserIds);
        groupRepository.assertGroupExists(request.getGroupId());
        groupRepository.assertUsersBelongToGroup(request.getGroupId(), allUserIds);

        ExpenseSplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        List<SplitAllocation> allocations = strategy.calculateSplit(
                request.getTotalAmount(),
                request.getPaidByUserId(),
                request.getParticipants()
        );

        UUID expenseId = expenseRepository.insertExpense(request);
        expenseRepository.batchInsertParticipants(expenseId, allocations);

        for (SplitAllocation allocation : allocations) {
            UUID participantId = allocation.getUserId();
            BigDecimal owed = allocation.getOwedAmount();

            if (participantId.equals(request.getPaidByUserId())) {
                continue;
            }

            if (owed.compareTo(BigDecimal.ZERO) > 0) {
                balanceRepository.applyDebt(
                        request.getGroupId(),
                        participantId,
                        request.getPaidByUserId(),
                        currencyCode,
                        owed,
                        "EXPENSE",
                        expenseId
                );
            }
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyRepository.attachReferenceId(idempotencyKey, REQUEST_TYPE, expenseId);
        }

        return new CreateExpenseResponse(
                expenseId,
                "SUCCESS",
                "Expense created and balances updated successfully"
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UpdateExpenseResponse updateExpense(UUID expenseId, @Valid UpdateExpenseRequest request) {
        ExpenseDetailResponse existing = expenseRepository.findExpenseById(expenseId);

        String newCurrencyCode = request.getCurrencyCode().toUpperCase();

        List<UUID> allUserIds = Stream.concat(
                Stream.of(request.getPaidByUserId()),
                request.getParticipants().stream().map(ParticipantShareRequest::getUserId)
        ).distinct().collect(Collectors.toList());

        userRepository.assertUsersExist(allUserIds);
        groupRepository.assertUsersBelongToGroup(existing.getGroupId(), allUserIds);

        // Undo the balance impact of the expense as it stood before this edit,
        // using its original payer/currency/participant shares.
        for (ExpenseParticipantResponse oldParticipant : existing.getParticipants()) {
            if (oldParticipant.getUserId().equals(existing.getPaidByUserId())) {
                continue;
            }
            BigDecimal owed = oldParticipant.getOwedAmount();
            if (owed.compareTo(BigDecimal.ZERO) > 0) {
                balanceRepository.applyDebt(
                        existing.getGroupId(),
                        existing.getPaidByUserId(),
                        oldParticipant.getUserId(),
                        existing.getCurrencyCode(),
                        owed,
                        "EXPENSE",
                        expenseId
                );
            }
        }

        ExpenseSplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        List<SplitAllocation> allocations = strategy.calculateSplit(
                request.getTotalAmount(),
                request.getPaidByUserId(),
                request.getParticipants()
        );

        expenseRepository.updateExpense(expenseId, request);
        expenseRepository.deleteParticipants(expenseId);
        expenseRepository.batchInsertParticipants(expenseId, allocations);

        // Re-apply the balance impact of the expense as it stands after this edit.
        for (SplitAllocation allocation : allocations) {
            UUID participantId = allocation.getUserId();
            BigDecimal owed = allocation.getOwedAmount();

            if (participantId.equals(request.getPaidByUserId())) {
                continue;
            }

            if (owed.compareTo(BigDecimal.ZERO) > 0) {
                balanceRepository.applyDebt(
                        existing.getGroupId(),
                        participantId,
                        request.getPaidByUserId(),
                        newCurrencyCode,
                        owed,
                        "EXPENSE",
                        expenseId
                );
            }
        }

        return new UpdateExpenseResponse(
                expenseId,
                "SUCCESS",
                "Expense updated and balances recalculated successfully"
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void deleteExpense(UUID expenseId) {
        ExpenseDetailResponse existing = expenseRepository.findExpenseById(expenseId);

        // Undo the balance impact this expense had, then remove it entirely.
        for (ExpenseParticipantResponse participant : existing.getParticipants()) {
            if (participant.getUserId().equals(existing.getPaidByUserId())) {
                continue;
            }
            BigDecimal owed = participant.getOwedAmount();
            if (owed.compareTo(BigDecimal.ZERO) > 0) {
                balanceRepository.applyDebt(
                        existing.getGroupId(),
                        existing.getPaidByUserId(),
                        participant.getUserId(),
                        existing.getCurrencyCode(),
                        owed,
                        "EXPENSE",
                        expenseId
                );
            }
        }

        expenseRepository.deleteExpense(expenseId);
    }

}
