import * as React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { groupApi } from "@/api/groupApi";
import { useToast } from "@/components/ui/toast";
import { AlertCircle } from "lucide-react";
import type { GroupDetail } from "@/types/group";

const schema = z.object({
  groupName: z.string().min(1, "Group name is required").max(150),
  description: z.string().max(2000).optional().or(z.literal("")),
});

type FormValues = z.infer<typeof schema>;

interface EditGroupDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  group: GroupDetail;
}

export function EditGroupDialog({ open, onOpenChange, group }: EditGroupDialogProps) {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [serverError, setServerError] = React.useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: { groupName: group.groupName, description: group.description ?? "" },
  });

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      groupApi.updateGroup(group.groupId, {
        groupName: values.groupName,
        description: values.description || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
      toast("Group updated", "success");
      onOpenChange(false);
    },
    onError: (err) => {
      setServerError(err instanceof Error ? err.message : "Failed to update group");
    },
  });

  const handleClose = () => {
    onOpenChange(false);
    reset();
    setServerError(null);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent onClose={handleClose}>
        <DialogHeader>
          <DialogTitle>Edit group</DialogTitle>
          <DialogDescription>Update the group name or description.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-4">
          {serverError && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}

          <div className="space-y-2">
            <Label htmlFor="edit-groupName">Group name</Label>
            <Input id="edit-groupName" {...register("groupName")} />
            {errors.groupName && (
              <p className="text-sm text-destructive">{errors.groupName.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="edit-description">Description</Label>
            <Input id="edit-description" {...register("description")} />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={handleClose}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting || mutation.isPending}>
              Save changes
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
