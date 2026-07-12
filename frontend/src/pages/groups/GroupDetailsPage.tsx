import { Link, useParams } from "react-router-dom";
import { useGroupDetail } from "@/hooks/useGroupDetail";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { OverviewTab } from "@/components/groups/OverviewTab";
import { ExpensesTab } from "@/components/groups/ExpensesTab";
import { BalancesTab } from "@/components/groups/BalancesTab";
import { SettlementsTab } from "@/components/groups/SettlementsTab";
import { MembersTab } from "@/components/groups/MembersTab";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/ui/error-state";
import { ArrowLeft } from "lucide-react";

export default function GroupDetailsPage() {
  const { groupId } = useParams<{ groupId: string }>();
  const { data: group, isLoading, isError } = useGroupDetail(groupId);

  return (
    <div className="space-y-4">
      <Link
        to="/groups"
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        All groups
      </Link>

      {isError ? (
        <ErrorState description="Couldn't load this group. It may not exist, or you may not have access." />
      ) : isLoading || !group ? (
        <div className="space-y-4">
          <Skeleton className="h-9 w-64" />
          <Skeleton className="h-10 w-80" />
          <Skeleton className="h-40" />
        </div>
      ) : (
        <>
          <div>
            <h1 className="text-2xl font-semibold">{group.groupName}</h1>
            <p className="text-sm text-muted-foreground">
              {group.members.length} member{group.members.length !== 1 ? "s" : ""}
            </p>
          </div>

          <Tabs defaultValue="overview">
            <TabsList>
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="expenses">Expenses</TabsTrigger>
              <TabsTrigger value="balances">Balances</TabsTrigger>
              <TabsTrigger value="settlements">Settlements</TabsTrigger>
              <TabsTrigger value="members">Members</TabsTrigger>
            </TabsList>

            <TabsContent value="overview">
              <OverviewTab group={group} />
            </TabsContent>
            <TabsContent value="expenses">
              <ExpensesTab groupId={group.groupId} groupName={group.groupName} members={group.members} />
            </TabsContent>
            <TabsContent value="balances">
              <BalancesTab groupId={group.groupId} />
            </TabsContent>
            <TabsContent value="settlements">
              <SettlementsTab groupId={group.groupId} groupName={group.groupName} members={group.members} />
            </TabsContent>
            <TabsContent value="members">
              <MembersTab group={group} />
            </TabsContent>
          </Tabs>
        </>
      )}
    </div>
  );
}
