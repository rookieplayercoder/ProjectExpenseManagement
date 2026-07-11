package com.prateek.ProjectExpenseManagement.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class SplitAllocation {
    private final UUID userId;
    private final BigDecimal owedAmount;
    private final BigDecimal percentageValue;
    private final BigDecimal exactAmountInput;

    public SplitAllocation(UUID userId, BigDecimal owedAmount, BigDecimal percentageValue, BigDecimal exactAmountInput) {
        this.userId = userId;
        this.owedAmount = owedAmount;
        this.percentageValue = percentageValue;
        this.exactAmountInput = exactAmountInput;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getOwedAmount() {
        return owedAmount;
    }

    public BigDecimal getPercentageValue() {
        return percentageValue;
    }

    public BigDecimal getExactAmountInput() {
        return exactAmountInput;
    }
}
