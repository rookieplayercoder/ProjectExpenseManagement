import * as React from "react";
import { Link } from "react-router-dom";
import { useGroupExpenses } from "@/hooks/useGroupActivity";
import { ExpenseCard } from "@/components/dashboard/ExpenseCard";
import { buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { cn } from "@/utils/cn";
import type { GroupMember } from "@/types/group";
import { Plus, Receipt, Search } from "lucide-react";

interface ExpensesTabProps {
  groupId: string;
  groupName: string;
  members: GroupMember[];
}

export function ExpensesTab({ groupId, groupName, members }: ExpensesTabProps) {
  const { data: expenses, isLoading, isError } = useGroupExpenses(groupId);
  const [query, setQuery] = React.useState("");
  const [memberFilter, setMemberFilter] = React.useState("ALL");
  const [splitTypeFilter, setSplitTypeFilter] = React.useState("ALL");
  const [dateFrom, setDateFrom] = React.useState("");
  const [dateTo, setDateTo] = React.useState("");
  const [sortBy, setSortBy] = React.useState("DATE_DESC");
  const [page, setPage] = React.useState(1);
  const pageSize = 10;

  React.useEffect(() => {
    setPage(1);
  }, [query, memberFilter, splitTypeFilter, dateFrom, dateTo, sortBy]);

  const filteredExpenses = React.useMemo(() => {
    if (!expenses) return expenses;
    const q = query.trim().toLowerCase();
    const filtered = expenses.filter((e) => {
      if (q && !e.title.toLowerCase().includes(q) && !e.paidByName.toLowerCase().includes(q)) return false;
      if (memberFilter !== "ALL" && e.paidByUserId !== memberFilter) return false;
      if (splitTypeFilter !== "ALL" && e.splitType !== splitTypeFilter) return false;
      if (dateFrom && e.expenseDate < dateFrom) return false;
      if (dateTo && e.expenseDate > dateTo) return false;
      return true;
    });
    const sorted = [...filtered];
    sorted.sort((a, b) => {
      switch (sortBy) {
        case "DATE_ASC":
          return a.expenseDate.localeCompare(b.expenseDate);
        case "AMOUNT_DESC":
          return b.totalAmount - a.totalAmount;
        case "AMOUNT_ASC":
          return a.totalAmount - b.totalAmount;
        default:
          return b.expenseDate.localeCompare(a.expenseDate);
      }
    });
    return sorted;
  }, [expenses, query, memberFilter, splitTypeFilter, dateFrom, dateTo, sortBy]);

  const totalPages = filteredExpenses ? Math.max(1, Math.ceil(filteredExpenses.length / pageSize)) : 1;
  const pagedExpenses = filteredExpenses?.slice((page - 1) * pageSize, page * pageSize);

  const hasActiveFilters = memberFilter !== "ALL" || splitTypeFilter !== "ALL" || dateFrom || dateTo;

  const selectClass =
    "h-10 rounded-md border border-input bg-background px-2.5 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring";

  const addExpenseButton = (
    <Link to={`/groups/${groupId}/expenses/new`} className={buttonVariants({ size: "sm" })}>
      <Plus className="h-4 w-4" />
      Add expense
    </Link>
  );

  if (isError) return <ErrorState description="Couldn't load expenses for this group." />;

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-16" />
        ))}
      </div>
    );
  }

  if (!expenses || expenses.length === 0) {
    return (
      <EmptyState
        icon={Receipt}
        title="No expenses yet"
        description="Add the first expense to start splitting costs in this group."
        action={
          <Link
            to={`/groups/${groupId}/expenses/new`}
            className={cn(buttonVariants({ size: "sm" }), "mt-2")}
          >
            <Plus className="h-4 w-4" />
            Add expense
          </Link>
        }
      />
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search expenses..."
            className="pl-8"
          />
        </div>
        {addExpenseButton}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <select
          value={memberFilter}
          onChange={(e) => setMemberFilter(e.target.value)}
          className={selectClass}
          aria-label="Filter by who paid"
        >
          <option value="ALL">Paid by: anyone</option>
          {members.map((m) => (
            <option key={m.userId} value={m.userId}>
              Paid by: {m.fullName}
            </option>
          ))}
        </select>
        <select
          value={splitTypeFilter}
          onChange={(e) => setSplitTypeFilter(e.target.value)}
          className={selectClass}
          aria-label="Filter by split type"
        >
          <option value="ALL">Split: any</option>
          <option value="EQUAL">Split: equal</option>
          <option value="EXACT">Split: exact</option>
          <option value="PERCENTAGE">Split: percentage</option>
        </select>
        <Input
          type="date"
          value={dateFrom}
          onChange={(e) => setDateFrom(e.target.value)}
          className="w-auto"
          aria-label="From date"
        />
        <Input
          type="date"
          value={dateTo}
          onChange={(e) => setDateTo(e.target.value)}
          className="w-auto"
          aria-label="To date"
        />
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value)}
          className={selectClass}
          aria-label="Sort expenses"
        >
          <option value="DATE_DESC">Newest first</option>
          <option value="DATE_ASC">Oldest first</option>
          <option value="AMOUNT_DESC">Amount: high to low</option>
          <option value="AMOUNT_ASC">Amount: low to high</option>
        </select>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={() => {
              setMemberFilter("ALL");
              setSplitTypeFilter("ALL");
              setDateFrom("");
              setDateTo("");
            }}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            Clear filters
          </button>
        )}
      </div>

      {filteredExpenses && filteredExpenses.length === 0 ? (
        <EmptyState icon={Search} title="No matching expenses" description="Try adjusting your search or filters." />
      ) : (
        <>
          <div className="space-y-2">
            {pagedExpenses!.map((e) => (
              <ExpenseCard key={e.expenseId} expense={{ ...e, groupName }} />
            ))}
          </div>
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-1 text-sm text-muted-foreground">
              <button
                type="button"
                disabled={page === 1}
                onClick={() => setPage((p) => p - 1)}
                className="disabled:opacity-40"
              >
                Previous
              </button>
              <span>
                Page {page} of {totalPages}
              </span>
              <button
                type="button"
                disabled={page === totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="disabled:opacity-40"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
