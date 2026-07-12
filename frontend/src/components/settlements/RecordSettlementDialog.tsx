import * as React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { settlementApi } from "@/api/settlementApi";
import { useAuth } from "@/hooks/useAuth";
import { useToast } from "@/components/ui/toast";
import { AlertCircle } from "lucide-react";
import type { BalanceEntry } from "@/types/group";

const schema = z.object({
  amount: z.coerce.number().positive("Enter an amount greater than 0"),
  currencyCode: z.string().min(1, "Currency is required").max(10),
  settlementDate: z.string().min(1, "Date is required"),
  note: z.string().max(500).optional().or(z.literal("")),
});

type FormValues = z.infer<typeof schema>;

interface RecordSettlementDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  groupId: string;
  prefill: BalanceEntry | null;
}

export function RecordSettlementDialog({
  open,
  onOpenChange,
  groupId,
  prefill,
}: RecordSettlementDialogProps) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [serverError, setServerError] = React.useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: {
      amount: prefill?.netAmount ?? 0,
      currencyCode: prefill?.currencyCode ?? "USD",
      settlementDate: new Date().toISOString().slice(0, 10),
      note: "",
    },
  });

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      if (!prefill || !user) throw new Error("Missing settlement participants");
      return settlementApi.settleBalance({
        groupId,
        paidByUserId: prefill.debtorUserId,
        paidToUserId: prefill.creditorUserId,
        amount: values.amount,
        currencyCode: values.currencyCode.toUpperCase(),
        settlementDate: values.settlementDate,
        note: values.note || undefined,
        createdByUserId: user.userId,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups", groupId] });
      toast("Settlement recorded", "success");
      onOpenChange(false);
    },
    onError: (err) => {
      setServerError(err instanceof Error ? err.message : "Failed to record settlement");
    },
  });

  const handleClose = () => {
    onOpenChange(false);
    reset();
    setServerError(null);
  };

  if (!prefill) return null;

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent onClose={handleClose}>
        <DialogHeader>
          <DialogTitle>Record settlement</DialogTitle>
          <DialogDescription>
            {prefill.debtorName} pays {prefill.creditorName}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-4">
          {serverError && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="settle-amount">Amount</Label>
              <Input id="settle-amount" type="number" step="0.01" min="0" {...register("amount")} />
              {errors.amount && <p className="text-sm text-destructive">{errors.amount.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="settle-currency">Currency</Label>
              <Input id="settle-currency" maxLength={10} {...register("currencyCode")} />
              {errors.currencyCode && (
                <p className="text-sm text-destructive">{errors.currencyCode.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="settle-date">Date</Label>
            <Input id="settle-date" type="date" {...register("settlementDate")} />
            {errors.settlementDate && (
              <p className="text-sm text-destructive">{errors.settlementDate.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="settle-note">Note (optional)</Label>
            <Input id="settle-note" placeholder="Paid via UPI" {...register("note")} />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={handleClose}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting || mutation.isPending}>
              Record settlement
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
