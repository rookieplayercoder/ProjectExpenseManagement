package com.prateek.ProjectExpenseManagement.strategy;

import com.prateek.ProjectExpenseManagement.domain.SplitAllocation;
import com.prateek.ProjectExpenseManagement.domain.SplitType;
import com.prateek.ProjectExpenseManagement.dto.ParticipantShareRequest;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import org.springframework.stereotype.Component;
import com.prateek.ProjectExpenseManagement.domain.SplitAllocation;
import com.prateek.ProjectExpenseManagement.domain.SplitType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class EqualSplitStrategy implements ExpenseSplitStrategy {

    private static final BigDecimal MIN_UNIT = new BigDecimal("0.0001");

    @Override
    public SplitType getSupportedType() {
        return SplitType.EQUAL;
    }

    @Override
    public List<SplitAllocation> calculateSplit(BigDecimal totalAmount,
                                                UUID paidByUserId,
                                                List<ParticipantShareRequest> participants) {
        validate(totalAmount, participants);

        List<ParticipantShareRequest> sorted = participants.stream()
                .sorted(Comparator.comparing(p -> p.getUserId().toString()))
                .toList();

        int size = sorted.size();
        BigDecimal baseShare = totalAmount.divide(BigDecimal.valueOf(size), 4, RoundingMode.DOWN);
        BigDecimal assigned = baseShare.multiply(BigDecimal.valueOf(size));
        BigDecimal remainder = totalAmount.subtract(assigned);

        List<SplitAllocation> allocations = new ArrayList<>();
        for (ParticipantShareRequest participant : sorted) {
            allocations.add(new SplitAllocation(
                    participant.getUserId(),
                    baseShare,
                    null,
                    null
            ));
        }

        int index = 0;
        while (remainder.compareTo(BigDecimal.ZERO) > 0) {
            SplitAllocation current = allocations.get(index);
            allocations.set(index, new SplitAllocation(
                    current.getUserId(),
                    current.getOwedAmount().add(MIN_UNIT),
                    null,
                    null
            ));
            remainder = remainder.subtract(MIN_UNIT);
            index++;
        }

        BigDecimal totalAllocated = allocations.stream()
                .map(SplitAllocation::getOwedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAllocated.compareTo(totalAmount) != 0) {
            throw new BusinessValidationException("Equal split computation failed total reconciliation");
        }

        return allocations;
    }

    private void validate(BigDecimal totalAmount, List<ParticipantShareRequest> participants) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Total amount must be greater than zero");
        }
        if (participants == null || participants.isEmpty()) {
            throw new BusinessValidationException("Participants cannot be empty");
        }

        Set<UUID> uniqueUsers = new HashSet<>();
        for (ParticipantShareRequest participant : participants) {
            if (!uniqueUsers.add(participant.getUserId())) {
                throw new BusinessValidationException("Duplicate participant userId: " + participant.getUserId());
            }
        }
    }
}
