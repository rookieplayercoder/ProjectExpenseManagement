package com.prateek.ProjectExpenseManagement.dto;

import com.prateek.ProjectExpenseManagement.domain.SplitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ExpenseSummaryResponse {

    private UUID expenseId;
    private String title;
    private BigDecimal totalAmount;
    private String currencyCode;
    private SplitType splitType;
    private UUID paidByUserId;
    private String paidByName;
    private LocalDate expenseDate;

    public ExpenseSummaryResponse(UUID expenseId, String title, BigDecimal totalAmount, String currencyCode,
                                  SplitType splitType, UUID paidByUserId, String paidByName, LocalDate expenseDate) {
        this.expenseId = expenseId;
        this.title = title;
        this.totalAmount = totalAmount;
        this.currencyCode = currencyCode;
        this.splitType = splitType;
        this.paidByUserId = paidByUserId;
        this.paidByName = paidByName;
        this.expenseDate = expenseDate;
    }

    public UUID getExpenseId() {
        return expenseId;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public UUID getPaidByUserId() {
        return paidByUserId;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }
}
