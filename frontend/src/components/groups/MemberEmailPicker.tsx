import * as React from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Avatar } from "@/components/ui/avatar";
import { userApi } from "@/api/userApi";
import { X, UserPlus, Loader2 } from "lucide-react";

export interface PickedMember {
  userId: string;
  fullName: string;
  email: string;
}

interface MemberEmailPickerProps {
  value: PickedMember[];
  onChange: (members: PickedMember[]) => void;
  excludeUserIds?: string[];
  label?: string;
}

export function MemberEmailPicker({
  value,
  onChange,
  excludeUserIds = [],
  label = "Add members by email",
}: MemberEmailPickerProps) {
  const [email, setEmail] = React.useState("");
  const [isSearching, setIsSearching] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const handleAdd = async () => {
    const trimmed = email.trim();
    if (!trimmed) return;
    setError(null);

    if (value.some((m) => m.email.toLowerCase() === trimmed.toLowerCase())) {
      setError("Already added");
      return;
    }

    setIsSearching(true);
    try {
      const found = await userApi.lookupByEmail(trimmed);
      if (excludeUserIds.includes(found.userId)) {
        setError("This person is already a member");
        return;
      }
      onChange([...value, found]);
      setEmail("");
    } catch {
      setError("No user found with that email");
    } finally {
      setIsSearching(false);
    }
  };

  const handleRemove = (userId: string) => {
    onChange(value.filter((m) => m.userId !== userId));
  };

  return (
    <div className="space-y-2">
      <label className="text-sm font-medium">{label}</label>
      <div className="flex gap-2">
        <Input
          type="email"
          placeholder="friend@example.com"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            setError(null);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              handleAdd();
            }
          }}
        />
        <Button type="button" variant="outline" onClick={handleAdd} disabled={isSearching}>
          {isSearching ? <Loader2 className="h-4 w-4 animate-spin" /> : <UserPlus className="h-4 w-4" />}
          Add
        </Button>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
      {value.length > 0 && (
        <div className="flex flex-wrap gap-2 pt-1">
          {value.map((m) => (
            <div
              key={m.userId}
              className="flex items-center gap-2 rounded-full border border-border bg-secondary py-1 pl-1 pr-2 text-sm"
            >
              <Avatar name={m.fullName} size="sm" />
              <span>{m.fullName}</span>
              <button
                type="button"
                onClick={() => handleRemove(m.userId)}
                className="text-muted-foreground hover:text-foreground"
                aria-label={`Remove ${m.fullName}`}
              >
                <X className="h-3 w-3" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
