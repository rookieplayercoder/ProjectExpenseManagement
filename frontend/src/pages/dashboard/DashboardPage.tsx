import { useDashboardData } from "@/hooks/useDashboardData";
import { BalanceCard } from "@/components/dashboard/BalanceCard";
import { ExpenseCard } from "@/components/dashboard/ExpenseCard";
import { SettlementCard } from "@/components/dashboard/SettlementCard";
import { GroupCard } from "@/components/dashboard/GroupCard";
import { MonthlySpendingChart, ExpenseDistributionChart, QuickActions } from "@/components/dashboard/DashboardCharts";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowDownCircle, ArrowUpCircle, Scale, Receipt, Users } from "lucide-react";

export default function DashboardPage() {
  const { data, isLoading, isError } = useDashboardData();

  if (isError) {
    return <ErrorState description="Couldn't load your dashboard. Please try again." />;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold">Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          A quick look at what you owe, what you're owed, and recent activity.
        </p>
      </div>

      {/* Balance summary cards */}
      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-28" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <BalanceCard
            title="Total You Owe"
            icon={ArrowUpCircle}
            totals={data?.youOwe ?? []}
            tone="negative"
          />
          <BalanceCard
            title="Total You Are Owed"
            icon={ArrowDownCircle}
            totals={data?.youAreOwed ?? []}
            tone="positive"
          />
          <BalanceCard title="Net Balance" icon={Scale} totals={data?.netBalance ?? []} />
          <BalanceCard title="Total Expenses" icon={Receipt} totals={data?.totalExpenses ?? []} />
        </div>
      )}

      <QuickActions />

      {!isLoading && data && (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <MonthlySpendingChart expenses={data.allExpenses} />
          <ExpenseDistributionChart expenses={data.allExpenses} />
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Recent expenses */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Recent Expenses</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {isLoading ? (
              Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-16" />)
            ) : data && data.recentExpenses.length > 0 ? (
              data.recentExpenses.map((e) => <ExpenseCard key={e.expenseId} expense={e} />)
            ) : (
              <EmptyState
                icon={Receipt}
                title="No expenses yet"
                description="Expenses you add to a group will show up here."
              />
            )}
          </CardContent>
        </Card>

        {/* Recent settlements */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Recent Settlements</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {isLoading ? (
              Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-16" />)
            ) : data && data.recentSettlements.length > 0 ? (
              data.recentSettlements.map((s) => (
                <SettlementCard key={s.settlementId} settlement={s} />
              ))
            ) : (
              <EmptyState
                icon={Scale}
                title="No settlements yet"
                description="Recorded settlements will show up here."
              />
            )}
          </CardContent>
        </Card>
      </div>

      {/* Groups summary */}
      <div className="space-y-3">
        <h2 className="text-lg font-semibold">Your Groups</h2>
        {isLoading ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-24" />
            ))}
          </div>
        ) : data && data.groups.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {data.groups.map((g) => (
              <GroupCard key={g.groupId} group={g} />
            ))}
          </div>
        ) : (
          <EmptyState
            icon={Users}
            title="No groups yet"
            description="Create a group to start tracking shared expenses. (Coming in the next step.)"
          />
        )}
      </div>
    </div>
  );
}
