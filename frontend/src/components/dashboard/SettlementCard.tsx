import { memo } from "react";
import { formatCurrency, formatDate } from "@/utils/format";
import { ArrowRight, HandCoins } from "lucide-react";
import type { SettlementSummary } from "@/types/settlement";

interface SettlementCardProps {
  settlement: SettlementSummary & { groupName: string };
}

export const SettlementCard = memo(function SettlementCard({ settlement }: SettlementCardProps) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-border p-3">
      <div className="flex items-center gap-3 overflow-hidden">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary">
          <HandCoins className="h-4 w-4 text-muted-foreground" />
        </div>
        <div className="min-w-0">
          <p className="flex items-center gap-1 truncate text-sm font-medium">
            {settlement.paidByName}
            <ArrowRight className="h-3 w-3 text-muted-foreground" />
            {settlement.paidToName}
          </p>
          <p className="truncate text-xs text-muted-foreground">
            {settlement.groupName} · {formatDate(settlement.settlementDate)}
          </p>
        </div>
      </div>
      <span className="shrink-0 text-sm font-semibold">
        {formatCurrency(settlement.amount, settlement.currencyCode)}
      </span>
    </div>
  );
});
