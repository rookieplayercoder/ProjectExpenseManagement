import * as React from "react";
import { useGroupBalances } from "@/hooks/useGroupActivity";
import { BalanceRow } from "@/components/groups/BalanceRow";
import { Avatar } from "@/components/ui/avatar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { RecordSettlementDialog } from "@/components/settlements/RecordSettlementDialog";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { formatCurrency } from "@/utils/format";
import { cn } from "@/utils/cn";
import { useAuth } from "@/hooks/useAuth";
import { Scale } from "lucide-react";
import type { BalanceEntry } from "@/types/group";

interface BalancesTabProps {
  groupId: string;
}

function NetBalanceSummary({ balances }: { balances: BalanceEntry[] }) {
  const nets = new Map<string, { name: string; byCurrency: Map<string, number> }>();
  const ensure = (id: string, name: string) => {
    if (!nets.has(id)) nets.set(id, { name, byCurrency: new Map() });
    return nets.get(id)!;
  };
  for (const b of balances) {
    const debtor = ensure(b.debtorUserId, b.debtorName);
    debtor.byCurrency.set(b.currencyCode, (debtor.byCurrency.get(b.currencyCode) ?? 0) - b.netAmount);
    const creditor = ensure(b.creditorUserId, b.creditorName);
    creditor.byCurrency.set(b.currencyCode, (creditor.byCurrency.get(b.currencyCode) ?? 0) + b.netAmount);
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Net Balance by Member</CardTitle>
      </CardHeader>
      <CardContent className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {Array.from(nets.entries()).map(([id, { name, byCurrency }]) => (
          <div key={id} className="flex items-center justify-between rounded-lg border border-border p-2.5">
            <div className="flex items-center gap-2">
              <Avatar name={name} size="sm" />
              <span className="text-sm font-medium">{name}</span>
            </div>
            <div className="text-right text-sm">
              {Array.from(byCurrency.entries()).map(([code, amt]) => (
                <p
                  key={code}
                  className={cn("font-semibold", amt > 0 ? "text-emerald-600" : amt < 0 ? "text-destructive" : "")}
                >
                  {amt >= 0 ? "+" : ""}
                  {formatCurrency(amt, code)}
                </p>
              ))}
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

export function BalancesTab({ groupId }: BalancesTabProps) {
  const { data, isLoading, isError } = useGroupBalances(groupId);
  const { user } = useAuth();
  const [settleEntry, setSettleEntry] = React.useState<BalanceEntry | null>(null);

  if (isError) return <ErrorState description="Couldn't load balances for this group." />;

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-14" />
        ))}
      </div>
    );
  }

  if (!data || data.balances.length === 0) {
    return (
      <EmptyState
        icon={Scale}
        title="All settled up"
        description="No outstanding balances in this group right now."
      />
    );
  }

  const maxAmount = Math.max(...data.balances.map((b) => b.netAmount));

  return (
    <div className="space-y-4">
      <NetBalanceSummary balances={data.balances} />
      <div className="space-y-2">
        {data.balances.map((entry, idx) => (
          <BalanceRow
            key={`${entry.debtorUserId}-${entry.creditorUserId}-${entry.currencyCode}-${idx}`}
            entry={entry}
            currentUserId={user?.userId}
            onSettle={setSettleEntry}
            maxAmount={maxAmount}
          />
        ))}
      </div>

      <RecordSettlementDialog
        open={!!settleEntry}
        onOpenChange={(open) => !open && setSettleEntry(null)}
        groupId={groupId}
        prefill={settleEntry}
      />
    </div>
  );
}
