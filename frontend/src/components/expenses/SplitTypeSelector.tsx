import { cn } from "@/utils/cn";
import type { SplitType } from "@/types/group";
import { Equal, Percent, Hash } from "lucide-react";

interface SplitTypeSelectorProps {
  value: SplitType;
  onChange: (value: SplitType) => void;
}

const options: { value: SplitType; label: string; icon: typeof Equal }[] = [
  { value: "EQUAL", label: "Equal", icon: Equal },
  { value: "EXACT", label: "Exact amounts", icon: Hash },
  { value: "PERCENTAGE", label: "Percentage", icon: Percent },
];

export function SplitTypeSelector({ value, onChange }: SplitTypeSelectorProps) {
  return (
    <div role="radiogroup" aria-label="Split type" className="grid grid-cols-3 gap-2">
      {options.map((opt) => {
        const Icon = opt.icon;
        const isActive = value === opt.value;
        return (
          <button
            key={opt.value}
            type="button"
            role="radio"
            aria-checked={isActive}
            onClick={() => onChange(opt.value)}
            className={cn(
              "flex flex-col items-center gap-1 rounded-lg border p-3 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
              isActive
                ? "border-primary bg-primary/5 text-primary"
                : "border-border text-muted-foreground hover:bg-secondary",
            )}
          >
            <Icon className="h-4 w-4" aria-hidden="true" />
            {opt.label}
          </button>
        );
      })}
    </div>
  );
}
