import * as React from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { AmountInput } from "@/components/expenses/AmountInput";
import { CurrencySelector } from "@/components/expenses/CurrencySelector";
import { SplitTypeSelector } from "@/components/expenses/SplitTypeSelector";
import { ParticipantSelector, type ParticipantValue } from "@/components/expenses/ParticipantSelector";
import { expenseApi, type ParticipantShareRequest } from "@/api/expenseApi";
import { useAuth } from "@/hooks/useAuth";
import { useToast } from "@/components/ui/toast";
import { AlertCircle } from "lucide-react";
import type { GroupMember } from "@/types/group";
import type { SplitType } from "@/types/group";
import type { ExpenseDetail } from "@/types/expense";

const scalarSchema = z.object({
  title: z.string().min(1, "Title is required").max(200),
  description: z.string().max(2000).optional().or(z.literal("")),
  totalAmount: z.coerce.number().positive("Enter an amount greater than 0"),
  currencyCode: z.string().min(1),
  expenseDate: z.string().min(1, "Date is required"),
  paidByUserId: z.string().min(1, "Choose who paid"),
});

type ScalarValues = z.infer<typeof scalarSchema>;

interface ExpenseFormProps {
  groupId: string;
  members: GroupMember[];
  /** When provided, the form edits this expense instead of creating a new one. */
  expense?: ExpenseDetail;
}

const EPSILON = 0.01;

function buildInitialParticipants(expense: ExpenseDetail | undefined): Record<string, ParticipantValue> {
  if (!expense) return {};
  const initial: Record<string, ParticipantValue> = {};
  for (const p of expense.participants) {
    initial[p.userId] = {
      userId: p.userId,
      included: true,
      exactAmount: p.exactAmountInput != null ? String(p.exactAmountInput) : undefined,
      percentage: p.percentageValue != null ? String(p.percentageValue) : undefined,
    };
  }
  return initial;
}

export function ExpenseForm({ groupId, members, expense }: ExpenseFormProps) {
  const isEditMode = !!expense;
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const [splitType, setSplitType] = React.useState<SplitType>(expense?.splitType ?? "EQUAL");
  const [participants, setParticipants] = React.useState<Record<string, ParticipantValue>>(
    buildInitialParticipants(expense),
  );
  const [participantsError, setParticipantsError] = React.useState<string | null>(null);
  const [serverError, setServerError] = React.useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    control,
    formState: { errors, isSubmitting },
  } = useForm<ScalarValues>({
    resolver: zodResolver(scalarSchema),
    defaultValues: {
      title: expense?.title ?? "",
      description: expense?.description ?? "",
      totalAmount: expense?.totalAmount,
      currencyCode: expense?.currencyCode ?? "USD",
      expenseDate: expense?.expenseDate ?? new Date().toISOString().slice(0, 10),
      paidByUserId: expense?.paidByUserId ?? user?.userId,
    },
  });

  const totalAmount = watch("totalAmount");

  const handleParticipantChange = (userId: string, patch: Partial<ParticipantValue>) => {
    setParticipants((prev) => ({
      ...prev,
      [userId]: { ...(prev[userId] ?? { userId, included: false }), ...patch },
    }));
    setParticipantsError(null);
  };

  const buildParticipantsPayload = (total: number): ParticipantShareRequest[] | null => {
    const included = Object.values(participants).filter((p) => p.included);

    if (included.length === 0) {
      setParticipantsError("Select at least one participant");
      return null;
    }

    if (splitType === "EQUAL") {
      return included.map((p) => ({ userId: p.userId }));
    }

    if (splitType === "EXACT") {
      const amounts = included.map((p) => ({ userId: p.userId, amount: parseFloat(p.exactAmount ?? "") }));
      if (amounts.some((a) => Number.isNaN(a.amount) || a.amount <= 0)) {
        setParticipantsError("Enter a valid amount for every selected participant");
        return null;
      }
      const sum = amounts.reduce((acc, a) => acc + a.amount, 0);
      if (Math.abs(sum - total) > EPSILON) {
        setParticipantsError(`Amounts must add up to the total (currently ${sum.toFixed(2)} of ${total.toFixed(2)})`);
        return null;
      }
      return amounts.map((a) => ({ userId: a.userId, exactAmount: a.amount }));
    }

    // PERCENTAGE
    const percentages = included.map((p) => ({ userId: p.userId, pct: parseFloat(p.percentage ?? "") }));
    if (percentages.some((p) => Number.isNaN(p.pct) || p.pct <= 0)) {
      setParticipantsError("Enter a valid percentage for every selected participant");
      return null;
    }
    const sum = percentages.reduce((acc, p) => acc + p.pct, 0);
    if (Math.abs(sum - 100) > EPSILON) {
      setParticipantsError(`Percentages must add up to 100 (currently ${sum.toFixed(2)}%)`);
      return null;
    }
    return percentages.map((p) => ({ userId: p.userId, percentage: p.pct }));
  };

  const createMutation = useMutation({
    mutationFn: (payload: Parameters<typeof expenseApi.createExpense>[0]) =>
      expenseApi.createExpense(payload, crypto.randomUUID()),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["groups", groupId] });
      toast("Expense added", "success");
      navigate(`/expenses/${data.expenseId}`);
    },
    onError: (err) => {
      setServerError(err instanceof Error ? err.message : "Failed to create expense");
    },
  });

  const updateMutation = useMutation({
    mutationFn: (payload: Parameters<typeof expenseApi.updateExpense>[1]) =>
      expenseApi.updateExpense(expense!.expenseId, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["groups", groupId] });
      queryClient.invalidateQueries({ queryKey: ["expenses", data.expenseId] });
      toast("Expense updated", "success");
      navigate(`/expenses/${data.expenseId}`);
    },
    onError: (err) => {
      setServerError(err instanceof Error ? err.message : "Failed to update expense");
    },
  });

  const mutation = isEditMode ? updateMutation : createMutation;

  const onSubmit = (values: ScalarValues) => {
    setServerError(null);
    setParticipantsError(null);

    const participantsPayload = buildParticipantsPayload(values.totalAmount);
    if (!participantsPayload) return;

    if (isEditMode) {
      updateMutation.mutate({
        paidByUserId: values.paidByUserId,
        title: values.title,
        description: values.description || undefined,
        totalAmount: values.totalAmount,
        currencyCode: values.currencyCode,
        splitType,
        expenseDate: values.expenseDate,
        participants: participantsPayload,
      });
    } else {
      createMutation.mutate({
        groupId,
        paidByUserId: values.paidByUserId,
        title: values.title,
        description: values.description || undefined,
        totalAmount: values.totalAmount,
        currencyCode: values.currencyCode,
        splitType,
        expenseDate: values.expenseDate,
        createdByUserId: user!.userId,
        participants: participantsPayload,
      });
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {serverError && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{serverError}</AlertDescription>
        </Alert>
      )}

      <div className="space-y-2">
        <Label htmlFor="title">Title</Label>
        <Input id="title" placeholder="Dinner at the beach shack" {...register("title")} />
        {errors.title && <p className="text-sm text-destructive">{errors.title.message}</p>}
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">Description (optional)</Label>
        <Input id="description" placeholder="Split evenly among everyone who came" {...register("description")} />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="totalAmount">Total amount</Label>
          <AmountInput id="totalAmount" {...register("totalAmount", { valueAsNumber: true })} />
          {errors.totalAmount && (
            <p className="text-sm text-destructive">{errors.totalAmount.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label htmlFor="currencyCode">Currency</Label>
          <Controller
            control={control}
            name="currencyCode"
            render={({ field }) => (
              <CurrencySelector id="currencyCode" value={field.value} onChange={field.onChange} />
            )}
          />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="expenseDate">Date</Label>
          <Input id="expenseDate" type="date" {...register("expenseDate")} />
          {errors.expenseDate && (
            <p className="text-sm text-destructive">{errors.expenseDate.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label htmlFor="paidByUserId">Paid by</Label>
          <select
            id="paidByUserId"
            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            {...register("paidByUserId")}
          >
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.fullName}
              </option>
            ))}
          </select>
          {errors.paidByUserId && (
            <p className="text-sm text-destructive">{errors.paidByUserId.message}</p>
          )}
        </div>
      </div>

      <div className="space-y-2">
        <Label>Split type</Label>
        <SplitTypeSelector value={splitType} onChange={setSplitType} />
      </div>

      <div className="space-y-2">
        <Label>Participants</Label>
        <ParticipantSelector
          members={members}
          splitType={splitType}
          values={participants}
          onChange={handleParticipantChange}
        />
        {participantsError && <p className="text-sm text-destructive">{participantsError}</p>}
        {splitType === "EXACT" && totalAmount > 0 && (
          <p className="text-xs text-muted-foreground">Amounts should add up to {totalAmount.toFixed(2)}</p>
        )}
        {splitType === "PERCENTAGE" && <p className="text-xs text-muted-foreground">Percentages should add up to 100%</p>}
      </div>

      <Button type="submit" className="w-full" isLoading={isSubmitting || mutation.isPending}>
        {isEditMode ? "Save changes" : "Add expense"}
      </Button>
    </form>
  );
}
