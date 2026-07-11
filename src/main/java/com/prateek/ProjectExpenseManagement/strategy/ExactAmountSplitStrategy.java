package com.prateek.ProjectExpenseManagement.strategy;

import com.prateek.ProjectExpenseManagement.domain.SplitAllocation;
import com.prateek.ProjectExpenseManagement.domain.SplitType;
import com.prateek.ProjectExpenseManagement.dto.ParticipantShareRequest;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import com.prateek.ProjectExpenseManagement.dto.ParticipantShareRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class ExactAmountSplitStrategy implements ExpenseSplitStrategy {

    @Override
    public SplitType getSupportedType() {
        return SplitType.EXACT;
    }

    @Override
    public List<SplitAllocation> calculateSplit(BigDecimal totalAmount,
                                                UUID paidByUserId,
                                                List<ParticipantShareRequest> participants) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Total amount must be greater than zero");
        }
        if (participants == null || participants.isEmpty()) {
            throw new BusinessValidationException("Participants cannot be empty");
        }

        Set<UUID> uniqueUsers = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;

        for (ParticipantShareRequest participant : participants) {
            if (participant.getUserId() == null) {
                throw new BusinessValidationException("Participant userId cannot be null");
            }
            if (!uniqueUsers.add(participant.getUserId())) {
                throw new BusinessValidationException("Duplicate participant userId: " + participant.getUserId());
            }
            if (participant.getExactAmount() == null) {
                throw new BusinessValidationException("Exact amount is required for EXACT split");
            }
            if (participant.getExactAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessValidationException("Exact amount cannot be negative");
            }
            sum = sum.add(participant.getExactAmount());
        }

        if (sum.compareTo(totalAmount) != 0) {
            throw new BusinessValidationException(
                    "Sum of exact amounts must equal total amount. Expected " + totalAmount + " but got " + sum
            );
        }

        return participants.stream()
                .map(p -> new SplitAllocation(
                        p.getUserId(),
                        p.getExactAmount(),
                        null,
                        p.getExactAmount()
                ))
                .toList();
    }
}
