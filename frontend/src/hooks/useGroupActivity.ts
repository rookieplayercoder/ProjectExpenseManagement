import { useQuery } from "@tanstack/react-query";
import { groupApi } from "@/api/groupApi";

export function useGroupExpenses(groupId: string | undefined) {
  return useQuery({
    queryKey: ["groups", groupId, "expenses"],
    queryFn: () => groupApi.getGroupExpenses(groupId!),
    enabled: !!groupId,
  });
}

export function useGroupBalances(groupId: string | undefined) {
  return useQuery({
    queryKey: ["groups", groupId, "balances"],
    queryFn: () => groupApi.getGroupBalances(groupId!),
    enabled: !!groupId,
  });
}

export function useGroupSettlements(groupId: string | undefined) {
  return useQuery({
    queryKey: ["groups", groupId, "settlements"],
    queryFn: () => groupApi.getGroupSettlements(groupId!),
    enabled: !!groupId,
  });
}
