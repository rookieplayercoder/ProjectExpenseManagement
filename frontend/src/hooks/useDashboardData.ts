import * as React from "react";
import { useQueries } from "@tanstack/react-query";
import { groupApi } from "@/api/groupApi";
import { useMyGroups } from "@/hooks/useMyGroups";
import { useAuth } from "@/hooks/useAuth";
import type { GroupSummary } from "@/types/group";
import type { ExpenseSummary } from "@/types/expense";
import type { SettlementSummary } from "@/types/settlement";

export interface CurrencyTotal {
  currencyCode: string;
  amount: number;
}

export interface DashboardData {
  groups: GroupSummary[];
  youOwe: CurrencyTotal[];
  youAreOwed: CurrencyTotal[];
  netBalance: CurrencyTotal[];
  totalExpenses: CurrencyTotal[];
  recentExpenses: (ExpenseSummary & { groupId: string; groupName: string })[];
  allExpenses: (ExpenseSummary & { groupId: string; groupName: string })[];
  recentSettlements: (SettlementSummary & { groupId: string; groupName: string })[];
}

function addToTotal(totals: Map<string, number>, currencyCode: string, amount: number) {
  totals.set(currencyCode, (totals.get(currencyCode) ?? 0) + amount);
}

function toSortedTotals(totals: Map<string, number>): CurrencyTotal[] {
  return Array.from(totals.entries())
    .map(([currencyCode, amount]) => ({ currencyCode, amount }))
    .sort((a, b) => a.currencyCode.localeCompare(b.currencyCode));
}

/**
 * There's no cross-group "my balances" or "my recent activity" endpoint on
 * the backend yet, so this hook fetches balances/expenses/settlements for
 * every group the user belongs to (in parallel) and aggregates client-side.
 * Fine for a handful of groups; would need a real aggregate endpoint to
 * scale further.
 */
export function useDashboardData() {
  const { user } = useAuth();
  const groupsQuery = useMyGroups();
  const groups = groupsQuery.data ?? [];

  const balanceQueries = useQueries({
    queries: groups.map((g) => ({
      queryKey: ["groups", g.groupId, "balances"],
      queryFn: () => groupApi.getGroupBalances(g.groupId),
      enabled: !!groupsQuery.data,
    })),
  });

  const expenseQueries = useQueries({
    queries: groups.map((g) => ({
      queryKey: ["groups", g.groupId, "expenses"],
      queryFn: () => groupApi.getGroupExpenses(g.groupId),
      enabled: !!groupsQuery.data,
    })),
  });

  const settlementQueries = useQueries({
    queries: groups.map((g) => ({
      queryKey: ["groups", g.groupId, "settlements"],
      queryFn: () => groupApi.getGroupSettlements(g.groupId),
      enabled: !!groupsQuery.data,
    })),
  });

  const isLoading =
    groupsQuery.isLoading ||
    balanceQueries.some((q) => q.isLoading) ||
    expenseQueries.some((q) => q.isLoading) ||
    settlementQueries.some((q) => q.isLoading);

  const isError =
    groupsQuery.isError ||
    balanceQueries.some((q) => q.isError) ||
    expenseQueries.some((q) => q.isError) ||
    settlementQueries.some((q) => q.isError);

  const data = React.useMemo<DashboardData | null>(() => {
    if (!user || !groupsQuery.data) return null;

    const youOwe = new Map<string, number>();
    const youAreOwed = new Map<string, number>();
    const totalExpenses = new Map<string, number>();
    const recentExpenses: DashboardData["recentExpenses"] = [];
    const recentSettlements: DashboardData["recentSettlements"] = [];

    groups.forEach((group, idx) => {
      const balances = balanceQueries[idx]?.data?.balances ?? [];
      for (const entry of balances) {
        if (entry.debtorUserId === user.userId) {
          addToTotal(youOwe, entry.currencyCode, entry.netAmount);
        }
        if (entry.creditorUserId === user.userId) {
          addToTotal(youAreOwed, entry.currencyCode, entry.netAmount);
        }
      }

      const expenses = expenseQueries[idx]?.data ?? [];
      for (const expense of expenses) {
        addToTotal(totalExpenses, expense.currencyCode, expense.totalAmount);
        recentExpenses.push({ ...expense, groupId: group.groupId, groupName: group.groupName });
      }

      const settlements = settlementQueries[idx]?.data ?? [];
      for (const settlement of settlements) {
        recentSettlements.push({ ...settlement, groupId: group.groupId, groupName: group.groupName });
      }
    });

    recentExpenses.sort((a, b) => b.expenseDate.localeCompare(a.expenseDate));
    recentSettlements.sort((a, b) => b.settlementDate.localeCompare(a.settlementDate));

    // Net balance = owed to you minus what you owe, per currency.
    const netBalance = new Map<string, number>();
    const currencies = new Set([...youOwe.keys(), ...youAreOwed.keys()]);
    for (const currency of currencies) {
      netBalance.set(currency, (youAreOwed.get(currency) ?? 0) - (youOwe.get(currency) ?? 0));
    }

    return {
      groups,
      youOwe: toSortedTotals(youOwe),
      youAreOwed: toSortedTotals(youAreOwed),
      netBalance: toSortedTotals(netBalance),
      totalExpenses: toSortedTotals(totalExpenses),
      recentExpenses: recentExpenses.slice(0, 5),
      allExpenses: recentExpenses,
      recentSettlements: recentSettlements.slice(0, 5),
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, groups, groupsQuery.data, balanceQueries, expenseQueries, settlementQueries]);

  return { data, isLoading, isError };
}
