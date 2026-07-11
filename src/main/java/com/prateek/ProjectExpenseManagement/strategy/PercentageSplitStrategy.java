package com.prateek.ProjectExpenseManagement.strategy;

import com.prateek.ProjectExpenseManagement.domain.SplitAllocation;
import com.prateek.ProjectExpenseManagement.domain.SplitType;
import com.prateek.ProjectExpenseManagement.dto.ParticipantShareRequest;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class PercentageSplitStrategy implements ExpenseSplitStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MIN_UNIT = new BigDecimal("0.0001");

    @Override
    public SplitType getSupportedType() {
        return SplitType.PERCENTAGE;
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

        BigDecimal percentageSum = BigDecimal.ZERO;
        Set<UUID> uniqueUsers = new HashSet<>();
        for (ParticipantShareRequest participant : participants) {
            if (!uniqueUsers.add(participant.getUserId())) {
                throw new BusinessValidationException("Duplicate participant userId: " + participant.getUserId());
            }
            if (participant.getPercentage() == null) {
                throw new BusinessValidationException("Percentage is required for PERCENTAGE split");
            }
            if (participant.getPercentage().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessValidationException("Percentage cannot be negative");
            }
            percentageSum = percentageSum.add(participant.getPercentage());
        }

        if (percentageSum.compareTo(HUNDRED) != 0) {
            throw new BusinessValidationException(
                    "Sum of all percentages must be exactly 100. Found: " + percentageSum
            );
        }

        List<ParticipantShareRequest> sorted = participants.stream()
                .sorted(Comparator.comparing(p -> p.getUserId().toString()))
                .toList();

        List<SplitAllocation> allocations = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO;

        for (ParticipantShareRequest participant : sorted) {
            BigDecimal share = totalAmount
                    .multiply(participant.getPercentage())
                    .divide(HUNDRED, 4, RoundingMode.DOWN);

            assigned = assigned.add(share);

            allocations.add(new SplitAllocation(
                    participant.getUserId(),
                    share,
                    participant.getPercentage(),
                    null
            ));
        }

        BigDecimal remainder = totalAmount.subtract(assigned);
        int index = 0;
        while (remainder.compareTo(BigDecimal.ZERO) > 0) {
            SplitAllocation current = allocations.get(index);
            allocations.set(index, new SplitAllocation(
                    current.getUserId(),
                    current.getOwedAmount().add(MIN_UNIT),
                    current.getPercentageValue(),
                    null
            ));
            remainder = remainder.subtract(MIN_UNIT);
            index++;
        }

        BigDecimal totalAllocated = allocations.stream()
                .map(SplitAllocation::getOwedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAllocated.compareTo(totalAmount) != 0) {
            throw new BusinessValidationException("Percentage split computation failed total reconciliation");
        }

        return allocations;
    }
}
