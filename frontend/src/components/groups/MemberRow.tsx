import * as React from "react";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { formatDate } from "@/utils/format";
import { UserMinus } from "lucide-react";
import type { GroupMember } from "@/types/group";

interface MemberRowProps {
  member: GroupMember;
  isCreator: boolean;
  isSelf: boolean;
  onRemove: (userId: string) => void;
  isRemoving: boolean;
}

export function MemberRow({ member, isCreator, isSelf, onRemove, isRemoving }: MemberRowProps) {
  const [confirmOpen, setConfirmOpen] = React.useState(false);

  return (
    <div className="flex items-center justify-between rounded-lg border border-border p-3">
      <div className="flex items-center gap-3">
        <Avatar name={member.fullName} />
        <div>
          <p className="flex items-center gap-2 text-sm font-medium">
            {member.fullName}
            {isCreator && (
              <Badge variant="secondary" className="text-[10px]">
                Creator
              </Badge>
            )}
            {isSelf && (
              <Badge variant="outline" className="text-[10px]">
                You
              </Badge>
            )}
          </p>
          <p className="text-xs text-muted-foreground">
            {member.email} · joined {formatDate(member.joinedAt)}
          </p>
        </div>
      </div>
      {!isCreator && (
        <>
          <Button
            variant="ghost"
            size="icon"
            className="text-muted-foreground hover:text-destructive"
            onClick={() => setConfirmOpen(true)}
            aria-label={`Remove ${member.fullName}`}
          >
            <UserMinus className="h-4 w-4" />
          </Button>
          <ConfirmDialog
            open={confirmOpen}
            onOpenChange={setConfirmOpen}
            title="Remove member"
            description={`Remove ${member.fullName} from this group? Their past expenses and settlements stay on record.`}
            confirmLabel="Remove"
            variant="destructive"
            isLoading={isRemoving}
            onConfirm={() => {
              onRemove(member.userId);
              setConfirmOpen(false);
            }}
          />
        </>
      )}
    </div>
  );
}
