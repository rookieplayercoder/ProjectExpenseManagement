import { Avatar } from "@/components/ui/avatar";
import { formatCurrency } from "@/utils/format";
import { ArrowRight } from "lucide-react";
import type { BalanceEntry } from "@/types/group";

interface BalanceRowProps {
  entry: BalanceEntry;
  currentUserId?: string;
  onSettle?: (entry: BalanceEntry) => void;
  maxAmount?: number;
}

export function BalanceRow({ entry, currentUserId, onSettle, maxAmount }: BalanceRowProps) {
  const involvesMe = entry.debtorUserId === currentUserId || entry.creditorUserId === currentUserId;
  const pct = maxAmount && maxAmount > 0 ? Math.min(100, (entry.netAmount / maxAmount) * 100) : 0;

  return (
    <div className="space-y-1.5 rounded-lg border border-border p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex min-w-0 items-center gap-2 text-sm">
          <Avatar name={entry.debtorName} size="sm" />
          <span className="truncate font-medium">{entry.debtorName}</span>
          <ArrowRight className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
          <Avatar name={entry.creditorName} size="sm" />
          <span className="truncate font-medium">{entry.creditorName}</span>
        </div>
        <div className="flex shrink-0 items-center gap-3">
          <span className="font-semibold">{formatCurrency(entry.netAmount, entry.currencyCode)}</span>
          {involvesMe && onSettle && (
            <button
              onClick={() => onSettle(entry)}
              className="text-xs font-medium text-primary hover:underline"
            >
              Settle up
            </button>
          )}
        </div>
      </div>
      {maxAmount != null && (
        <div className="h-1 w-full overflow-hidden rounded-full bg-secondary">
          <div className="h-full rounded-full bg-destructive/70" style={{ width: `${pct}%` }} />
        </div>
      )}
    </div>
  );
}
