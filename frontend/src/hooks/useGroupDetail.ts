import { useQuery } from "@tanstack/react-query";
import { groupApi } from "@/api/groupApi";

export function useGroupDetail(groupId: string | undefined) {
  return useQuery({
    queryKey: ["groups", groupId, "detail"],
    queryFn: () => groupApi.getGroupDetail(groupId!),
    enabled: !!groupId,
  });
}
