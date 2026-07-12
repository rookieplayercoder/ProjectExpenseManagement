package com.prateek.ProjectExpenseManagement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class ParticipantShareRequest {

    @NotNull
    private UUID userId;

    @DecimalMin(value = "0.0000", inclusive = true)
    @Digits(integer = 15, fraction = 4, message = "exactAmount may have at most 15 integer digits and 4 decimal places")
    private BigDecimal exactAmount;

    // The strategy still enforces that all percentages sum to exactly 100 -
    // these bounds just reject an obviously-invalid single value early.
    @DecimalMin(value = "0.00", inclusive = true)
    @DecimalMax(value = "100.00", inclusive = true)
    private BigDecimal percentage;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getExactAmount() {
        return exactAmount;
    }

    public void setExactAmount(BigDecimal exactAmount) {
        this.exactAmount = exactAmount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
