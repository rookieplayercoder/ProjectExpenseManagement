import * as React from "react";
import { useMyGroups } from "@/hooks/useMyGroups";
import { GroupCard } from "@/components/dashboard/GroupCard";
import { CreateGroupDialog } from "@/components/groups/CreateGroupDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ErrorState } from "@/components/ui/error-state";
import { Plus, Search, Users } from "lucide-react";

export default function GroupsPage() {
  const { data: groups, isLoading, isError } = useMyGroups();
  const [createOpen, setCreateOpen] = React.useState(false);
  const [query, setQuery] = React.useState("");

  const filteredGroups = React.useMemo(() => {
    if (!groups) return groups;
    const q = query.trim().toLowerCase();
    if (!q) return groups;
    return groups.filter(
      (g) =>
        g.groupName.toLowerCase().includes(q) ||
        (g.description ?? "").toLowerCase().includes(q),
    );
  }, [groups, query]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Your Groups</h1>
          <p className="text-sm text-muted-foreground">Manage the groups you split expenses in.</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4" />
          Create Group
        </Button>
      </div>

      {!isError && !isLoading && groups && groups.length > 0 && (
        <div className="relative max-w-sm">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search groups..."
            className="pl-8"
          />
        </div>
      )}

      {isError ? (
        <ErrorState description="Couldn't load your groups. Please try again." />
      ) : isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      ) : groups && groups.length > 0 ? (
        filteredGroups && filteredGroups.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filteredGroups.map((g) => (
              <GroupCard key={g.groupId} group={g} />
            ))}
          </div>
        ) : (
          <EmptyState icon={Search} title="No matching groups" description="Try a different search term." />
        )
      ) : (
        <EmptyState
          icon={Users}
          title="No groups yet"
          description="Create your first group to start tracking shared expenses."
          action={
            <Button className="mt-2" onClick={() => setCreateOpen(true)}>
              <Plus className="h-4 w-4" />
              Create Group
            </Button>
          }
        />
      )}

      <CreateGroupDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}
