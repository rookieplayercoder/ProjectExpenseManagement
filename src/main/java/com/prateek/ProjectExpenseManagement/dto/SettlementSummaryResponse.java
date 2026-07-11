package com.prateek.ProjectExpenseManagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class SettlementSummaryResponse {

    private UUID settlementId;
    private UUID paidByUserId;
    private String paidByName;
    private UUID paidToUserId;
    private String paidToName;
    private BigDecimal amount;
    private String currencyCode;
    private LocalDate settlementDate;
    private String note;

    public SettlementSummaryResponse(UUID settlementId, UUID paidByUserId, String paidByName,
                                     UUID paidToUserId, String paidToName, BigDecimal amount,
                                     String currencyCode, LocalDate settlementDate, String note) {
        this.settlementId = settlementId;
        this.paidByUserId = paidByUserId;
        this.paidByName = paidByName;
        this.paidToUserId = paidToUserId;
        this.paidToName = paidToName;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.settlementDate = settlementDate;
        this.note = note;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public UUID getPaidByUserId() {
        return paidByUserId;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public UUID getPaidToUserId() {
        return paidToUserId;
    }

    public String getPaidToName() {
        return paidToName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public String getNote() {
        return note;
    }
}
