import * as React from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useExpenseDetail } from "@/hooks/useExpenseDetail";
import { expenseApi } from "@/api/expenseApi";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/ui/error-state";
import { Button, buttonVariants } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useToast } from "@/components/ui/toast";
import { formatCurrency, formatDate } from "@/utils/format";
import { cn } from "@/utils/cn";
import { ArrowLeft, Pencil, Receipt, Trash2 } from "lucide-react";

export default function ExpenseDetailsPage() {
  const { expenseId } = useParams<{ expenseId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const { data: expense, isLoading, isError } = useExpenseDetail(expenseId);
  const [isDeleteOpen, setIsDeleteOpen] = React.useState(false);

  const deleteMutation = useMutation({
    mutationFn: () => expenseApi.deleteExpense(expenseId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups", expense?.groupId] });
      toast("Expense deleted", "success");
      navigate(expense?.groupId ? `/groups/${expense.groupId}` : "/groups");
    },
    onError: (err) => {
      toast(err instanceof Error ? err.message : "Failed to delete expense", "error");
      setIsDeleteOpen(false);
    },
  });

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <Link
        to={expense?.groupId ? `/groups/${expense.groupId}` : "/groups"}
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to group
      </Link>

      {isError ? (
        <ErrorState description="Couldn't load this expense." />
      ) : isLoading || !expense ? (
        <div className="space-y-4">
          <Skeleton className="h-32" />
          <Skeleton className="h-48" />
        </div>
      ) : (
        <>
          <Card>
            <CardHeader className="flex flex-row flex-wrap items-start justify-between gap-3 space-y-0">
              <div className="flex min-w-0 items-start gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary">
                  <Receipt className="h-5 w-5 text-muted-foreground" />
                </div>
                <div className="min-w-0">
                  <CardTitle className="text-lg break-words">{expense.title}</CardTitle>
                  {expense.description && (
                    <p className="mt-1 text-sm text-muted-foreground">{expense.description}</p>
                  )}
                </div>
              </div>
              <div className="flex shrink-0 flex-wrap items-center gap-2">
                <Badge variant="outline">{expense.splitType}</Badge>
                {expense.groupId && (
                  <Link
                    to={`/expenses/${expense.expenseId}/edit`}
                    className={cn(buttonVariants({ variant: "outline", size: "sm" }))}
                  >
                    <Pencil className="h-3.5 w-3.5" />
                    Edit
                  </Link>
                )}
                <Button variant="outline" size="sm" onClick={() => setIsDeleteOpen(true)}>
                  <Trash2 className="h-3.5 w-3.5" />
                  Delete
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-2">
              <p className="text-3xl font-semibold">
                {formatCurrency(expense.totalAmount, expense.currencyCode)}
              </p>
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <Avatar name={expense.paidByName} size="sm" />
                  Paid by {expense.paidByName}
                </span>
                <span>{formatDate(expense.expenseDate)}</span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Split breakdown</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {expense.participants.map((p) => {
                const pct = expense.totalAmount > 0 ? (p.owedAmount / expense.totalAmount) * 100 : 0;
                const isPayer = p.userId === expense.paidByUserId;
                return (
                  <div key={p.userId} className="space-y-1.5 rounded-lg border border-border p-2.5">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Avatar name={p.userName} size="sm" />
                        <span className="text-sm font-medium">{p.userName}</span>
                        {isPayer && <Badge variant="outline">Paid</Badge>}
                        {expense.splitType === "PERCENTAGE" && p.percentageValue != null && (
                          <span className="text-xs text-muted-foreground">({p.percentageValue}%)</span>
                        )}
                      </div>
                      <span className="text-sm font-semibold">
                        {formatCurrency(p.owedAmount, expense.currencyCode)}
                      </span>
                    </div>
                    <div className="h-1.5 w-full overflow-hidden rounded-full bg-secondary">
                      <div
                        className="h-full rounded-full bg-primary"
                        style={{ width: `${Math.min(100, pct)}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </CardContent>
          </Card>

          <ConfirmDialog
            open={isDeleteOpen}
            onOpenChange={setIsDeleteOpen}
            title="Delete this expense?"
            description={`This will remove "${expense.title}" and reverse the balances it created. This can't be undone.`}
            confirmLabel="Delete"
            variant="destructive"
            isLoading={deleteMutation.isPending}
            onConfirm={() => deleteMutation.mutate()}
          />
        </>
      )}
    </div>
  );
}
