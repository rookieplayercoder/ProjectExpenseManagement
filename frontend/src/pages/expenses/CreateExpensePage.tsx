import { Link, useParams } from "react-router-dom";
import { useGroupDetail } from "@/hooks/useGroupDetail";
import { ExpenseForm } from "@/components/expenses/ExpenseForm";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/ui/error-state";
import { ArrowLeft } from "lucide-react";

export default function CreateExpensePage() {
  const { groupId } = useParams<{ groupId: string }>();
  const { data: group, isLoading, isError } = useGroupDetail(groupId);

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <Link
        to={groupId ? `/groups/${groupId}` : "/groups"}
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to group
      </Link>

      {isError ? (
        <ErrorState description="Couldn't load this group." />
      ) : isLoading || !group ? (
        <Skeleton className="h-96" />
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>Add an expense to {group.groupName}</CardTitle>
          </CardHeader>
          <CardContent>
            <ExpenseForm groupId={group.groupId} members={group.members} />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
