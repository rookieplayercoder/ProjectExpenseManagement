package com.prateek.ProjectExpenseManagement.dto;

import com.prateek.ProjectExpenseManagement.domain.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Same shape as CreateExpenseRequest, minus groupId/createdByUserId - an edit
// can't move an expense to a different group or change who originally logged it.
public class UpdateExpenseRequest {

    @NotNull
    private UUID paidByUserId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    @Digits(integer = 15, fraction = 4, message = "totalAmount may have at most 15 integer digits and 4 decimal places")
    private BigDecimal totalAmount;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "currencyCode must be a 3-letter ISO 4217 code, e.g. USD")
    private String currencyCode;

    @NotNull
    private SplitType splitType;

    @NotNull
    private LocalDate expenseDate;

    @Valid
    @NotEmpty
    @Size(max = 100, message = "A single expense cannot have more than 100 participants")
    private List<ParticipantShareRequest> participants;

    public UUID getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(UUID paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public List<ParticipantShareRequest> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantShareRequest> participants) {
        this.participants = participants;
    }
}
