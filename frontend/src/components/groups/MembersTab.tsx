import * as React from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { MemberRow } from "@/components/groups/MemberRow";
import { AddMemberDialog } from "@/components/groups/AddMemberDialog";
import { EmptyState } from "@/components/ui/empty-state";
import { useToast } from "@/components/ui/toast";
import { useAuth } from "@/hooks/useAuth";
import { groupApi } from "@/api/groupApi";
import { Search, UserPlus } from "lucide-react";
import type { GroupDetail } from "@/types/group";

interface MembersTabProps {
  group: GroupDetail;
}

export function MembersTab({ group }: MembersTabProps) {
  const [addOpen, setAddOpen] = React.useState(false);
  const [removingId, setRemovingId] = React.useState<string | null>(null);
  const [query, setQuery] = React.useState("");
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const removeMutation = useMutation({
    mutationFn: (userId: string) => groupApi.removeMember(group.groupId, userId),
    onMutate: (userId) => setRemovingId(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups", group.groupId] });
      toast("Member removed", "success");
    },
    onError: (err) => {
      toast(err instanceof Error ? err.message : "Failed to remove member", "error");
    },
    onSettled: () => setRemovingId(null),
  });

  const filteredMembers = React.useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return group.members;
    return group.members.filter(
      (m) => m.fullName.toLowerCase().includes(q) || m.email.toLowerCase().includes(q),
    );
  }, [group.members, query]);

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        {group.members.length > 1 && (
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search members..."
              className="pl-8"
            />
          </div>
        )}
        <Button size="sm" className="ml-auto" onClick={() => setAddOpen(true)}>
          <UserPlus className="h-4 w-4" />
          Add member
        </Button>
      </div>

      {filteredMembers.length === 0 ? (
        <EmptyState icon={Search} title="No matching members" description="Try a different search term." />
      ) : (
        <div className="space-y-2">
          {filteredMembers.map((member) => (
            <MemberRow
              key={member.userId}
              member={member}
              isCreator={member.userId === group.createdByUserId}
              isSelf={member.userId === user?.userId}
              onRemove={(userId) => removeMutation.mutate(userId)}
              isRemoving={removingId === member.userId && removeMutation.isPending}
            />
          ))}
        </div>
      )}

      <AddMemberDialog
        open={addOpen}
        onOpenChange={setAddOpen}
        groupId={group.groupId}
        existingMemberIds={group.members.map((m) => m.userId)}
      />
    </div>
  );
}
