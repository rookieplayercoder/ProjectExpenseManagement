package com.prateek.ProjectExpenseManagement.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class BalanceEntryResponse {

    private UUID debtorUserId;
    private String debtorName;
    private UUID creditorUserId;
    private String creditorName;
    private String currencyCode;
    private BigDecimal netAmount;

    public BalanceEntryResponse(UUID debtorUserId, String debtorName,
                                UUID creditorUserId, String creditorName,
                                String currencyCode, BigDecimal netAmount) {
        this.debtorUserId = debtorUserId;
        this.debtorName = debtorName;
        this.creditorUserId = creditorUserId;
        this.creditorName = creditorName;
        this.currencyCode = currencyCode;
        this.netAmount = netAmount;
    }

    public UUID getDebtorUserId() {
        return debtorUserId;
    }

    public String getDebtorName() {
        return debtorName;
    }

    public UUID getCreditorUserId() {
        return creditorUserId;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }
}
