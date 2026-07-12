import { useQuery } from "@tanstack/react-query";
import { groupApi } from "@/api/groupApi";

export function useMyGroups() {
  return useQuery({
    queryKey: ["groups", "mine"],
    queryFn: groupApi.listMyGroups,
  });
}
