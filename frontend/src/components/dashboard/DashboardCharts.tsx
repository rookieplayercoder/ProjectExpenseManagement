import { memo, useMemo } from "react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatCurrency } from "@/utils/format";
import { Plus, Users, Scale } from "lucide-react";
import type { ExpenseSummary } from "@/types/expense";

type Expense = ExpenseSummary & { groupId: string; groupName: string };

function lastNMonths(n: number): { key: string; label: string }[] {
  const out = [];
  const now = new Date();
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    out.push({
      key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`,
      label: d.toLocaleDateString(undefined, { month: "short" }),
    });
  }
  return out;
}

export const MonthlySpendingChart = memo(function MonthlySpendingChart({ expenses }: { expenses: Expense[] }) {
  const { months, values, max } = useMemo(() => {
    const months = lastNMonths(6);
    const totals = new Map<string, number>();
    for (const e of expenses) {
      const key = e.expenseDate.slice(0, 7);
      totals.set(key, (totals.get(key) ?? 0) + e.totalAmount);
    }
    const values = months.map((m) => totals.get(m.key) ?? 0);
    return { months, values, max: Math.max(1, ...values) };
  }, [expenses]);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Monthly Spending</CardTitle>
      </CardHeader>
      <CardContent>
        {expenses.length === 0 ? (
          <p className="text-sm text-muted-foreground">No expenses yet to chart.</p>
        ) : (
          <div className="flex h-32 items-end gap-3">
            {months.map((m, i) => (
              <div key={m.key} className="flex flex-1 flex-col items-center gap-1">
                <div
                  className="w-full rounded-t-md bg-primary/80"
                  style={{ height: `${Math.max(4, (values[i] / max) * 100)}%` }}
                  title={values[i].toFixed(2)}
                />
                <span className="text-xs text-muted-foreground">{m.label}</span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
});

export const ExpenseDistributionChart = memo(function ExpenseDistributionChart({ expenses }: { expenses: Expense[] }) {
  const { rows, max } = useMemo(() => {
    const totals = new Map<string, number>();
    for (const e of expenses) {
      totals.set(e.groupName, (totals.get(e.groupName) ?? 0) + e.totalAmount);
    }
    const rows = Array.from(totals.entries()).sort((a, b) => b[1] - a[1]);
    return { rows, max: Math.max(1, ...rows.map(([, v]) => v)) };
  }, [expenses]);
  const currency = expenses[0]?.currencyCode ?? "USD";

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Spending by Group</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2.5">
        {rows.length === 0 ? (
          <p className="text-sm text-muted-foreground">No expenses yet to break down.</p>
        ) : (
          rows.map(([name, amount]) => (
            <div key={name} className="space-y-1">
              <div className="flex justify-between text-sm">
                <span className="font-medium">{name}</span>
                <span className="text-muted-foreground">{formatCurrency(amount, currency)}</span>
              </div>
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-secondary">
                <div className="h-full rounded-full bg-primary" style={{ width: `${(amount / max) * 100}%` }} />
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
});

const QUICK_ACTIONS = [
  { to: "/groups", label: "Add expense", icon: Plus },
  { to: "/groups", label: "Create group", icon: Users },
  { to: "/groups", label: "Settle up", icon: Scale },
];

export const QuickActions = memo(function QuickActions() {
  const actions = QUICK_ACTIONS;
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
      {actions.map((a) => (
        <Link
          key={a.label}
          to={a.to}
          className="flex items-center gap-2 rounded-xl border border-border bg-card p-3.5 text-sm font-medium shadow-sm transition-colors hover:bg-secondary"
        >
          <a.icon className="h-4 w-4 text-primary" />
          {a.label}
        </Link>
      ))}
    </div>
  );
});
