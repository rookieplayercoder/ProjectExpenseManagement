import * as React from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import { cn } from "@/utils/cn";

interface DialogContextValue {
  titleId: string;
  descriptionId: string;
}

const DialogContext = React.createContext<DialogContextValue | undefined>(undefined);

const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

interface DialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
}

export function Dialog({ open, onOpenChange, children }: DialogProps) {
  const reactId = React.useId();
  const titleId = `dialog-title-${reactId}`;
  const descriptionId = `dialog-desc-${reactId}`;
  const contentRef = React.useRef<HTMLDivElement>(null);
  const previouslyFocused = React.useRef<HTMLElement | null>(null);

  // Move focus into the dialog on open, and restore it to whatever triggered
  // the dialog once it closes, so keyboard users don't lose their place.
  React.useEffect(() => {
    if (!open) return;
    previouslyFocused.current = document.activeElement as HTMLElement | null;

    const focusables = contentRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR);
    (focusables?.[0] ?? contentRef.current)?.focus();

    return () => {
      previouslyFocused.current?.focus?.();
    };
  }, [open]);

  React.useEffect(() => {
    if (!open) return;

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onOpenChange(false);
        return;
      }
      if (e.key !== "Tab" || !contentRef.current) return;

      // Basic focus trap: cycle Tab/Shift+Tab within the dialog.
      const focusables = Array.from(
        contentRef.current.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR),
      ).filter((el) => el.offsetParent !== null);
      if (focusables.length === 0) return;

      const first = focusables[0];
      const last = focusables[focusables.length - 1];
      const active = document.activeElement;

      if (e.shiftKey && active === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && active === last) {
        e.preventDefault();
        first.focus();
      }
    };

    document.addEventListener("keydown", onKeyDown);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = "";
    };
  }, [open, onOpenChange]);

  if (!open) return null;

  return createPortal(
    <DialogContext.Provider value={{ titleId, descriptionId }}>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div
          className="fixed inset-0 bg-black/50 backdrop-blur-[1px]"
          onClick={() => onOpenChange(false)}
          aria-hidden
        />
        <div ref={contentRef} className="relative z-10 flex max-h-full w-full max-w-md flex-col">
          {children}
        </div>
      </div>
    </DialogContext.Provider>,
    document.body,
  );
}

export function DialogContent({
  className,
  children,
  onClose,
}: {
  className?: string;
  children: React.ReactNode;
  onClose?: () => void;
}) {
  const ctx = React.useContext(DialogContext);
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby={ctx?.titleId}
      aria-describedby={ctx?.descriptionId}
      tabIndex={-1}
      className={cn(
        "relative flex w-full flex-col overflow-y-auto rounded-xl border border-border bg-card p-6 text-card-foreground shadow-lg focus:outline-none",
        className,
      )}
      style={{ maxHeight: "85vh" }}
    >
      {onClose && (
        <button
          onClick={onClose}
          className="absolute right-4 top-4 rounded-sm text-muted-foreground hover:text-foreground"
          aria-label="Close"
        >
          <X className="h-4 w-4" />
        </button>
      )}
      {children}
    </div>
  );
}

export function DialogHeader({ children }: { children: React.ReactNode }) {
  return <div className="mb-4 space-y-1">{children}</div>;
}

export function DialogTitle({ children }: { children: React.ReactNode }) {
  const ctx = React.useContext(DialogContext);
  return (
    <h2 id={ctx?.titleId} className="text-lg font-semibold">
      {children}
    </h2>
  );
}

export function DialogDescription({ children }: { children: React.ReactNode }) {
  const ctx = React.useContext(DialogContext);
  return (
    <p id={ctx?.descriptionId} className="text-sm text-muted-foreground">
      {children}
    </p>
  );
}

export function DialogFooter({ children }: { children: React.ReactNode }) {
  return <div className="mt-6 flex justify-end gap-2">{children}</div>;
}
