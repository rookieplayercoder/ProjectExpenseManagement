package com.prateek.ProjectExpenseManagement.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ExpenseParticipantResponse {

    private UUID userId;
    private String userName;
    private BigDecimal owedAmount;
    private BigDecimal percentageValue;
    private BigDecimal exactAmountInput;

    public ExpenseParticipantResponse(UUID userId, String userName, BigDecimal owedAmount,
                                      BigDecimal percentageValue, BigDecimal exactAmountInput) {
        this.userId = userId;
        this.userName = userName;
        this.owedAmount = owedAmount;
        this.percentageValue = percentageValue;
        this.exactAmountInput = exactAmountInput;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
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
