package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.dto.CreateExpenseRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateExpenseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TransactionalExpenseFacade {

    private final TransactionTemplate transactionTemplate;
    private final ExpenseService expenseService;

    public TransactionalExpenseFacade(PlatformTransactionManager transactionManager,
                                      ExpenseService expenseService) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.expenseService = expenseService;
    }

    public CreateExpenseResponse createExpense(CreateExpenseRequest request, String idempotencyKey) {
        return transactionTemplate.execute(status -> {
            try {
                return expenseService.createExpense(request, idempotencyKey);
            } catch (Exception ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });
    }
}
