import { Avatar } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { cn } from "@/utils/cn";
import type { GroupMember } from "@/types/group";
import type { SplitType } from "@/types/group";

export interface ParticipantValue {
  userId: string;
  included: boolean;
  exactAmount?: string;
  percentage?: string;
}

interface ParticipantSelectorProps {
  members: GroupMember[];
  splitType: SplitType;
  values: Record<string, ParticipantValue>;
  onChange: (userId: string, patch: Partial<ParticipantValue>) => void;
}

export function ParticipantSelector({ members, splitType, values, onChange }: ParticipantSelectorProps) {
  return (
    <div className="space-y-2">
      {members.map((member) => {
        const v = values[member.userId] ?? { userId: member.userId, included: false };
        return (
          <div
            key={member.userId}
            className={cn(
              "flex items-center justify-between gap-3 rounded-lg border p-2.5 transition-colors",
              v.included ? "border-primary/40 bg-primary/5" : "border-border",
            )}
          >
            <label className="flex min-w-0 flex-1 items-center gap-2.5">
              <input
                type="checkbox"
                checked={v.included}
                onChange={(e) => onChange(member.userId, { included: e.target.checked })}
                className="h-4 w-4 shrink-0 rounded border-input accent-primary"
              />
              <Avatar name={member.fullName} size="sm" />
              <span className="truncate text-sm font-medium">{member.fullName}</span>
            </label>

            {v.included && splitType === "EXACT" && (
              <Input
                type="number"
                step="0.01"
                min="0"
                placeholder="0.00"
                aria-label={`Amount for ${member.fullName}`}
                className="w-24 shrink-0"
                value={v.exactAmount ?? ""}
                onChange={(e) => onChange(member.userId, { exactAmount: e.target.value })}
              />
            )}
            {v.included && splitType === "PERCENTAGE" && (
              <Input
                type="number"
                step="0.01"
                min="0"
                max="100"
                placeholder="0%"
                aria-label={`Percentage for ${member.fullName}`}
                className="w-20 shrink-0"
                value={v.percentage ?? ""}
                onChange={(e) => onChange(member.userId, { percentage: e.target.value })}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}
