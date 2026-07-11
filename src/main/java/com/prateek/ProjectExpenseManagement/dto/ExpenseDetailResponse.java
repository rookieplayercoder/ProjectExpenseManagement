package com.prateek.ProjectExpenseManagement.dto;

import com.prateek.ProjectExpenseManagement.domain.SplitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ExpenseDetailResponse {

    private UUID expenseId;
    private UUID groupId;
    private UUID paidByUserId;
    private String paidByName;
    private String title;
    private String description;
    private BigDecimal totalAmount;
    private String currencyCode;
    private SplitType splitType;
    private LocalDate expenseDate;
    private UUID createdByUserId;
    private Instant createdAt;
    private List<ExpenseParticipantResponse> participants;

    public ExpenseDetailResponse(UUID expenseId, UUID groupId, UUID paidByUserId, String paidByName,
                                 String title, String description, BigDecimal totalAmount,
                                 String currencyCode, SplitType splitType, LocalDate expenseDate,
                                 UUID createdByUserId, Instant createdAt,
                                 List<ExpenseParticipantResponse> participants) {
        this.expenseId = expenseId;
        this.groupId = groupId;
        this.paidByUserId = paidByUserId;
        this.paidByName = paidByName;
        this.title = title;
        this.description = description;
        this.totalAmount = totalAmount;
        this.currencyCode = currencyCode;
        this.splitType = splitType;
        this.expenseDate = expenseDate;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.participants = participants;
    }

    public UUID getExpenseId() {
        return expenseId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getPaidByUserId() {
        return paidByUserId;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ExpenseParticipantResponse> getParticipants() {
        return participants;
    }
}
