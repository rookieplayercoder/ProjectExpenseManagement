import { memo } from "react";
import { Link } from "react-router-dom";
import { formatCurrency, formatDate } from "@/utils/format";
import { Badge } from "@/components/ui/badge";
import { Receipt } from "lucide-react";
import type { ExpenseSummary } from "@/types/expense";

interface ExpenseCardProps {
  expense: ExpenseSummary & { groupName: string };
}

export const ExpenseCard = memo(function ExpenseCard({ expense }: ExpenseCardProps) {
  return (
    <Link
      to={`/expenses/${expense.expenseId}`}
      className="flex items-center justify-between gap-3 rounded-lg border border-border p-3 transition-colors hover:bg-secondary/50"
    >
      <div className="flex items-center gap-3 overflow-hidden">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary">
          <Receipt className="h-4 w-4 text-muted-foreground" />
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{expense.title}</p>
          <p className="truncate text-xs text-muted-foreground">
            {expense.groupName} · paid by {expense.paidByName} · {formatDate(expense.expenseDate)}
          </p>
        </div>
      </div>
      <div className="flex shrink-0 flex-col items-end gap-1">
        <span className="text-sm font-semibold">
          {formatCurrency(expense.totalAmount, expense.currencyCode)}
        </span>
        <Badge variant="outline" className="text-[10px]">
          {expense.splitType}
        </Badge>
      </div>
    </Link>
  );
});
