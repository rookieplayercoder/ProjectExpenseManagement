import * as React from "react";
import { useNavigate } from "react-router-dom";
import { Search, Users, X } from "lucide-react";
import { useMyGroups } from "@/hooks/useMyGroups";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { cn } from "@/utils/cn";

export function GlobalSearch() {
  const [open, setOpen] = React.useState(false);
  const [query, setQuery] = React.useState("");
  const { data: groups } = useMyGroups();
  const navigate = useNavigate();
  const containerRef = React.useRef<HTMLDivElement>(null);
  const inputRef = React.useRef<HTMLInputElement>(null);

  React.useEffect(() => {
    if (!open) return;
    const onClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onClickOutside);
    document.addEventListener("keydown", onKeyDown);
    inputRef.current?.focus();
    return () => {
      document.removeEventListener("mousedown", onClickOutside);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  const q = query.trim().toLowerCase();
  const results =
    q.length > 0
      ? (groups ?? []).filter(
          (g) => g.groupName.toLowerCase().includes(q) || (g.description ?? "").toLowerCase().includes(q),
        )
      : [];

  const goToGroup = (groupId: string) => {
    setOpen(false);
    setQuery("");
    navigate(`/groups/${groupId}`);
  };

  return (
    <div ref={containerRef} className="relative">
      {open ? (
        <div className="relative w-40 sm:w-56">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            ref={inputRef}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search groups..."
            aria-label="Search groups"
            role="combobox"
            aria-expanded={q.length > 0}
            aria-controls="global-search-results"
            className="h-9 pl-8 pr-7"
          />
          <button
            type="button"
            onClick={() => {
              setOpen(false);
              setQuery("");
            }}
            aria-label="Close search"
            className="absolute right-1.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
          >
            <X className="h-3.5 w-3.5" />
          </button>

          {q.length > 0 && (
            <div
              id="global-search-results"
              role="listbox"
              aria-label="Search results"
              aria-live="polite"
              className="absolute right-0 top-full z-20 mt-1 w-64 rounded-lg border border-border bg-card p-1 shadow-lg sm:w-72"
            >
              {results.length === 0 ? (
                <p className="p-2.5 text-sm text-muted-foreground">No groups match "{query}"</p>
              ) : (
                results.slice(0, 6).map((g) => (
                  <button
                    key={g.groupId}
                    type="button"
                    role="option"
                    aria-selected={false}
                    onClick={() => goToGroup(g.groupId)}
                    className={cn(
                      "flex w-full items-center gap-2 rounded-md px-2.5 py-2 text-left text-sm hover:bg-secondary",
                    )}
                  >
                    <Users className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <span className="truncate font-medium">{g.groupName}</span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>
      ) : (
        <Button variant="ghost" size="icon" onClick={() => setOpen(true)} aria-label="Search groups">
          <Search className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}
