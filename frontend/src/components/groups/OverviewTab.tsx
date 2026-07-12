import * as React from "react";
import { Button } from "@/components/ui/button";
import { Avatar } from "@/components/ui/avatar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { EditGroupDialog } from "@/components/groups/EditGroupDialog";
import { formatDate } from "@/utils/format";
import { Pencil, Calendar, Users } from "lucide-react";
import type { GroupDetail } from "@/types/group";

interface OverviewTabProps {
  group: GroupDetail;
}

export function OverviewTab({ group }: OverviewTabProps) {
  const [editOpen, setEditOpen] = React.useState(false);

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex flex-row items-start justify-between space-y-0">
          <div>
            <CardTitle className="text-base">About this group</CardTitle>
          </div>
          <Button variant="outline" size="sm" onClick={() => setEditOpen(true)}>
            <Pencil className="h-3.5 w-3.5" />
            Edit
          </Button>
        </CardHeader>
        <CardContent className="space-y-3">
          <p className="text-sm text-muted-foreground">
            {group.description || "No description added yet."}
          </p>
          <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <Calendar className="h-3.5 w-3.5" />
              Created {formatDate(group.createdAt)}
            </span>
            <span className="flex items-center gap-1.5">
              <Users className="h-3.5 w-3.5" />
              {group.members.length} member{group.members.length !== 1 ? "s" : ""}
            </span>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Members</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-3">
          {group.members.map((m) => (
            <div key={m.userId} className="flex items-center gap-2">
              <Avatar name={m.fullName} size="sm" />
              <span className="text-sm">{m.fullName}</span>
            </div>
          ))}
        </CardContent>
      </Card>

      <EditGroupDialog open={editOpen} onOpenChange={setEditOpen} group={group} />
    </div>
  );
}
