import { AlertTriangle } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";

interface ServerErrorPageProps {
  onRetry?: () => void;
}

export default function ServerErrorPage({ onRetry }: ServerErrorPageProps) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 text-center">
      <AlertTriangle className="h-10 w-10 text-destructive" />
      <h1 className="text-4xl font-bold">500</h1>
      <p className="max-w-xs text-muted-foreground">
        Something went wrong on our end. Please try again.
      </p>
      <button
        onClick={onRetry ?? (() => window.location.reload())}
        className={buttonVariants({ variant: "default" })}
      >
        Reload page
      </button>
    </div>
  );
}
