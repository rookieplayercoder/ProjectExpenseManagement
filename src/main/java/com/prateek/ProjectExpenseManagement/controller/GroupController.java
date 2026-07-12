package com.prateek.ProjectExpenseManagement.controller;

import com.prateek.ProjectExpenseManagement.dto.CreateGroupRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateGroupResponse;
import com.prateek.ProjectExpenseManagement.dto.ExpenseSummaryResponse;
import com.prateek.ProjectExpenseManagement.dto.GroupBalanceResponse;
import com.prateek.ProjectExpenseManagement.dto.GroupSummaryResponse;
import com.prateek.ProjectExpenseManagement.dto.SettlementSummaryResponse;
import com.prateek.ProjectExpenseManagement.security.AuthenticatedUser;
import com.prateek.ProjectExpenseManagement.service.BalanceService;
import com.prateek.ProjectExpenseManagement.service.ExpenseQueryService;
import com.prateek.ProjectExpenseManagement.service.GroupService;
import com.prateek.ProjectExpenseManagement.service.SettlementQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final BalanceService balanceService;
    private final GroupService groupService;
    private final ExpenseQueryService expenseQueryService;
    private final SettlementQueryService settlementQueryService;

    public GroupController(BalanceService balanceService, GroupService groupService,
                           ExpenseQueryService expenseQueryService,
                           SettlementQueryService settlementQueryService) {
        this.balanceService = balanceService;
        this.groupService = groupService;
        this.expenseQueryService = expenseQueryService;
        this.settlementQueryService = settlementQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request);
    }

    /**
     * Groups the authenticated caller is an active member of. The caller is
     * identified from the JWT (see JwtAuthenticationFilter), not a query param,
     * so a user can never list another user's groups.
     */
    @GetMapping
    public List<GroupSummaryResponse> getMyGroups(@AuthenticationPrincipal AuthenticatedUser principal) {
        return groupService.getGroupsForUser(principal.userId());
    }

    @GetMapping("/{groupId}/balances")
    public GroupBalanceResponse getGroupBalances(@PathVariable UUID groupId) {
        return balanceService.getGroupBalances(groupId);
    }

    @GetMapping("/{groupId}/expenses")
    public List<ExpenseSummaryResponse> getGroupExpenses(@PathVariable UUID groupId) {
        return expenseQueryService.getExpensesForGroup(groupId);
    }

    @GetMapping("/{groupId}/settlements")
    public List<SettlementSummaryResponse> getGroupSettlements(@PathVariable UUID groupId) {
        return settlementQueryService.getSettlementsForGroup(groupId);
    }
}
