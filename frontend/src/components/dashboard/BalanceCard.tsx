import { memo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatCurrency } from "@/utils/format";
import type { CurrencyTotal } from "@/hooks/useDashboardData";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/utils/cn";

interface BalanceCardProps {
  title: string;
  icon: LucideIcon;
  totals: CurrencyTotal[];
  tone?: "neutral" | "positive" | "negative";
}

const toneClasses: Record<NonNullable<BalanceCardProps["tone"]>, string> = {
  neutral: "text-foreground",
  positive: "text-emerald-600",
  negative: "text-destructive",
};

export const BalanceCard = memo(function BalanceCard({ title, icon: Icon, totals, tone = "neutral" }: BalanceCardProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        {totals.length === 0 ? (
          <p className={cn("text-2xl font-semibold", toneClasses.neutral)}>
            {formatCurrency(0, "USD")}
          </p>
        ) : (
          <div className="space-y-0.5">
            {totals.map((t) => (
              <p key={t.currencyCode} className={cn("text-2xl font-semibold", toneClasses[tone])}>
                {formatCurrency(t.amount, t.currencyCode)}
              </p>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
});
