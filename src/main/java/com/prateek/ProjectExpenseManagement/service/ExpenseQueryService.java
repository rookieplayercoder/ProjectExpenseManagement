package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.dto.ExpenseDetailResponse;
import com.prateek.ProjectExpenseManagement.dto.ExpenseSummaryResponse;
import com.prateek.ProjectExpenseManagement.repository.ExpenseRepository;
import com.prateek.ProjectExpenseManagement.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ExpenseQueryService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;

    public ExpenseQueryService(ExpenseRepository expenseRepository, GroupRepository groupRepository) {
        this.expenseRepository = expenseRepository;
        this.groupRepository = groupRepository;
    }

    public ExpenseDetailResponse getExpense(UUID expenseId) {
        return expenseRepository.findExpenseById(expenseId);
    }

    public List<ExpenseSummaryResponse> getExpensesForGroup(UUID groupId) {
        groupRepository.assertGroupExists(groupId);
        return expenseRepository.findExpensesByGroupId(groupId);
    }
}
