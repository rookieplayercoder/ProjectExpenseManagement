import * as React from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { MemberEmailPicker, type PickedMember } from "@/components/groups/MemberEmailPicker";
import { groupApi } from "@/api/groupApi";
import { useToast } from "@/components/ui/toast";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { AlertCircle } from "lucide-react";

interface AddMemberDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  groupId: string;
  existingMemberIds: string[];
}

export function AddMemberDialog({ open, onOpenChange, groupId, existingMemberIds }: AddMemberDialogProps) {
  const [members, setMembers] = React.useState<PickedMember[]>([]);
  const [error, setError] = React.useState<string | null>(null);
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: () => groupApi.addMembers(groupId, { userIds: members.map((m) => m.userId) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups", groupId] });
      toast(members.length > 1 ? "Members added" : "Member added", "success");
      setMembers([]);
      onOpenChange(false);
    },
    onError: (err) => {
      setError(err instanceof Error ? err.message : "Failed to add members");
    },
  });

  const handleClose = () => {
    onOpenChange(false);
    setMembers([]);
    setError(null);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent onClose={handleClose}>
        <DialogHeader>
          <DialogTitle>Add members</DialogTitle>
          <DialogDescription>Invite people by their account email.</DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <MemberEmailPicker value={members} onChange={setMembers} excludeUserIds={existingMemberIds} />
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            onClick={() => {
              setError(null);
              mutation.mutate();
            }}
            disabled={members.length === 0}
            isLoading={mutation.isPending}
          >
            Add {members.length > 0 ? `(${members.length})` : ""}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
