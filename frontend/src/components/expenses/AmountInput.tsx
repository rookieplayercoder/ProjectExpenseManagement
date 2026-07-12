import * as React from "react";
import { Input } from "@/components/ui/input";

interface AmountInputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "type"> {}

export const AmountInput = React.forwardRef<HTMLInputElement, AmountInputProps>((props, ref) => {
  return <Input ref={ref} type="number" step="0.01" min="0" inputMode="decimal" placeholder="0.00" {...props} />;
});
AmountInput.displayName = "AmountInput";
