import * as React from "react";
import { useGroupSettlements } from "@/hooks/useGroupActivity";
import { SettlementCard } from "@/components/dashboard/SettlementCard";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { HandCoins, Search } from "lucide-react";
import type { GroupMember } from "@/types/group";

interface SettlementsTabProps {
  groupId: string;
  groupName: string;
  members: GroupMember[];
}

export function SettlementsTab({ groupId, groupName, members }: SettlementsTabProps) {
  const { data: settlements, isLoading, isError } = useGroupSettlements(groupId);
  const [query, setQuery] = React.useState("");
  const [memberFilter, setMemberFilter] = React.useState("ALL");
  const [dateFrom, setDateFrom] = React.useState("");
  const [dateTo, setDateTo] = React.useState("");
  const [sortBy, setSortBy] = React.useState("DATE_DESC");

  const selectClass =
    "h-10 rounded-md border border-input bg-background px-2.5 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring";

  const filteredSettlements = React.useMemo(() => {
    if (!settlements) return settlements;
    const q = query.trim().toLowerCase();
    const filtered = settlements.filter((s) => {
      if (
        q &&
        !s.paidByName.toLowerCase().includes(q) &&
        !s.paidToName.toLowerCase().includes(q) &&
        !(s.note ?? "").toLowerCase().includes(q)
      )
        return false;
      if (memberFilter !== "ALL" && s.paidByUserId !== memberFilter && s.paidToUserId !== memberFilter)
        return false;
      if (dateFrom && s.settlementDate < dateFrom) return false;
      if (dateTo && s.settlementDate > dateTo) return false;
      return true;
    });
    const sorted = [...filtered];
    sorted.sort((a, b) => {
      switch (sortBy) {
        case "DATE_ASC":
          return a.settlementDate.localeCompare(b.settlementDate);
        case "AMOUNT_DESC":
          return b.amount - a.amount;
        case "AMOUNT_ASC":
          return a.amount - b.amount;
        default:
          return b.settlementDate.localeCompare(a.settlementDate);
      }
    });
    return sorted;
  }, [settlements, query, memberFilter, dateFrom, dateTo, sortBy]);

  const hasActiveFilters = memberFilter !== "ALL" || dateFrom || dateTo;

  if (isError) return <ErrorState description="Couldn't load settlements for this group." />;

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-16" />
        ))}
      </div>
    );
  }

  if (!settlements || settlements.length === 0) {
    return (
      <EmptyState
        icon={HandCoins}
        title="No settlements yet"
        description="Recorded settlements will show up here."
      />
    );
  }

  return (
    <div className="space-y-3">
      <div className="relative">
        <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search settlements..."
          className="pl-8"
        />
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <select
          value={memberFilter}
          onChange={(e) => setMemberFilter(e.target.value)}
          className={selectClass}
          aria-label="Filter by member involved"
        >
          <option value="ALL">Involving: anyone</option>
          {members.map((m) => (
            <option key={m.userId} value={m.userId}>
              Involving: {m.fullName}
            </option>
          ))}
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
          aria-label="Sort settlements"
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
              setDateFrom("");
              setDateTo("");
            }}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            Clear filters
          </button>
        )}
      </div>

      {filteredSettlements && filteredSettlements.length === 0 ? (
        <EmptyState icon={Search} title="No matching settlements" description="Try adjusting your search or filters." />
      ) : (
        <div className="space-y-2">
          {filteredSettlements!.map((s) => (
            <SettlementCard key={s.settlementId} settlement={{ ...s, groupName }} />
          ))}
        </div>
      )}
    </div>
  );
}
