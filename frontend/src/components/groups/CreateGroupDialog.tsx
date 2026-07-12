import * as React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { MemberEmailPicker, type PickedMember } from "@/components/groups/MemberEmailPicker";
import { groupApi } from "@/api/groupApi";
import { useAuth } from "@/hooks/useAuth";
import { useToast } from "@/components/ui/toast";
import { AlertCircle } from "lucide-react";

const schema = z.object({
  groupName: z.string().min(1, "Group name is required").max(150),
  description: z.string().max(2000).optional().or(z.literal("")),
});

type FormValues = z.infer<typeof schema>;

interface CreateGroupDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CreateGroupDialog({ open, onOpenChange }: CreateGroupDialogProps) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [members, setMembers] = React.useState<PickedMember[]>([]);
  const [serverError, setServerError] = React.useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const createMutation = useMutation({
    mutationFn: (values: FormValues) =>
      groupApi.createGroup({
        groupName: values.groupName,
        description: values.description || undefined,
        createdByUserId: user!.userId,
        memberUserIds: members.map((m) => m.userId),
      }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
      toast("Group created!", "success");
      onOpenChange(false);
      reset();
      setMembers([]);
      navigate(`/groups/${data.groupId}`);
    },
    onError: (err) => {
      setServerError(err instanceof Error ? err.message : "Failed to create group");
    },
  });

  const onSubmit = (values: FormValues) => {
    setServerError(null);
    createMutation.mutate(values);
  };

  const handleClose = () => {
    onOpenChange(false);
    reset();
    setMembers([]);
    setServerError(null);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent onClose={handleClose}>
        <DialogHeader>
          <DialogTitle>Create a group</DialogTitle>
          <DialogDescription>Add friends by email to start splitting expenses.</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {serverError && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}

          <div className="space-y-2">
            <Label htmlFor="groupName">Group name</Label>
            <Input id="groupName" placeholder="Goa Trip" {...register("groupName")} />
            {errors.groupName && (
              <p className="text-sm text-destructive">{errors.groupName.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description (optional)</Label>
            <Input id="description" placeholder="Weekend getaway costs" {...register("description")} />
          </div>

          <MemberEmailPicker value={members} onChange={setMembers} />

          <DialogFooter>
            <Button type="button" variant="outline" onClick={handleClose}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting || createMutation.isPending}>
              Create group
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
