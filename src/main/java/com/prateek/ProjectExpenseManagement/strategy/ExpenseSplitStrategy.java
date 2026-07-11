package com.prateek.ProjectExpenseManagement.strategy;

import com.prateek.ProjectExpenseManagement.domain.SplitAllocation;
import com.prateek.ProjectExpenseManagement.domain.SplitType;
import com.prateek.ProjectExpenseManagement.dto.ParticipantShareRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ExpenseSplitStrategy {

    SplitType getSupportedType();

    List<SplitAllocation> calculateSplit(
            BigDecimal totalAmount,
            UUID paidByUserId,
            List<ParticipantShareRequest> participants
    );
}
