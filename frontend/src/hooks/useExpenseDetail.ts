import { useQuery } from "@tanstack/react-query";
import { expenseApi } from "@/api/expenseApi";

export function useExpenseDetail(expenseId: string | undefined) {
  return useQuery({
    queryKey: ["expenses", expenseId],
    queryFn: () => expenseApi.getExpense(expenseId!),
    enabled: !!expenseId,
  });
}
