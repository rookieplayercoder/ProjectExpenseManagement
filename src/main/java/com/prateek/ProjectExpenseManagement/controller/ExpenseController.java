package com.prateek.ProjectExpenseManagement.controller;

import com.prateek.ProjectExpenseManagement.dto.CreateExpenseRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateExpenseResponse;
import com.prateek.ProjectExpenseManagement.dto.ExpenseDetailResponse;
import com.prateek.ProjectExpenseManagement.dto.UpdateExpenseRequest;
import com.prateek.ProjectExpenseManagement.dto.UpdateExpenseResponse;
import com.prateek.ProjectExpenseManagement.service.ExpenseQueryService;
import com.prateek.ProjectExpenseManagement.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseQueryService expenseQueryService;

    public ExpenseController(ExpenseService expenseService, ExpenseQueryService expenseQueryService) {
        this.expenseService = expenseService;
        this.expenseQueryService = expenseQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateExpenseResponse createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return expenseService.createExpense(request, idempotencyKey);
    }

    @GetMapping("/{expenseId}")
    public ExpenseDetailResponse getExpense(@PathVariable UUID expenseId) {
        return expenseQueryService.getExpense(expenseId);
    }

    @PutMapping("/{expenseId}")
    public UpdateExpenseResponse updateExpense(
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateExpenseRequest request) {
        return expenseService.updateExpense(expenseId, request);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable UUID expenseId) {
        expenseService.deleteExpense(expenseId);
    }
}
