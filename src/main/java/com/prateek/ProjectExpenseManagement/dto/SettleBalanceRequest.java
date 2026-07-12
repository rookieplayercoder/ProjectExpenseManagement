package com.prateek.ProjectExpenseManagement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class SettleBalanceRequest {

    private UUID groupId;

    @NotNull
    private UUID paidByUserId;

    @NotNull
    private UUID paidToUserId;

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    @Digits(integer = 15, fraction = 4, message = "amount may have at most 15 integer digits and 4 decimal places")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "currencyCode must be a 3-letter ISO 4217 code, e.g. USD")
    private String currencyCode;

    @NotNull
    private LocalDate settlementDate;

    @Size(max = 2000)
    private String note;

    @NotNull
    private UUID createdByUserId;

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(UUID paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public UUID getPaidToUserId() {
        return paidToUserId;
    }

    public void setPaidToUserId(UUID paidToUserId) {
        this.paidToUserId = paidToUserId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}
