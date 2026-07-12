import { memo } from "react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Users } from "lucide-react";
import type { GroupSummary } from "@/types/group";

interface GroupCardProps {
  group: GroupSummary;
}

export const GroupCard = memo(function GroupCard({ group }: GroupCardProps) {
  return (
    <Link to={`/groups/${group.groupId}`}>
      <Card className="transition-shadow hover:shadow-md">
        <CardHeader className="pb-2">
          <CardTitle className="text-base">{group.groupName}</CardTitle>
        </CardHeader>
        <CardContent className="flex items-center justify-between">
          <p className="line-clamp-1 text-sm text-muted-foreground">
            {group.description || "No description"}
          </p>
          <div className="flex shrink-0 items-center gap-1 text-sm text-muted-foreground">
            <Users className="h-3.5 w-3.5" />
            {group.memberCount}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
});
