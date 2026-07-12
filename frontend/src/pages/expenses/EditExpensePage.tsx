import { Link, useParams, Navigate } from "react-router-dom";
import { useExpenseDetail } from "@/hooks/useExpenseDetail";
import { useGroupDetail } from "@/hooks/useGroupDetail";
import { ExpenseForm } from "@/components/expenses/ExpenseForm";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/ui/error-state";
import { ArrowLeft } from "lucide-react";

export default function EditExpensePage() {
  const { expenseId } = useParams<{ expenseId: string }>();
  const { data: expense, isLoading: isExpenseLoading, isError: isExpenseError } = useExpenseDetail(expenseId);
  const { data: group, isLoading: isGroupLoading, isError: isGroupError } = useGroupDetail(
    expense?.groupId ?? undefined,
  );

  const isLoading = isExpenseLoading || (!!expense?.groupId && isGroupLoading);
  const isError = isExpenseError || isGroupError;

  // Expenses without a group can't be edited here yet (no member list to split against).
  if (expense && !expense.groupId) {
    return <Navigate to={`/expenses/${expenseId}`} replace />;
  }

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <Link
        to={expense?.groupId ? `/expenses/${expenseId}` : "/groups"}
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to expense
      </Link>

      {isError ? (
        <ErrorState description="Couldn't load this expense." />
      ) : isLoading || !expense || !group ? (
        <Skeleton className="h-96" />
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>Edit expense</CardTitle>
          </CardHeader>
          <CardContent>
            <ExpenseForm groupId={group.groupId} members={group.members} expense={expense} />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
