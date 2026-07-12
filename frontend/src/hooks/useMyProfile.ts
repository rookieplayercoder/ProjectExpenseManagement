import { useQuery } from "@tanstack/react-query";
import { userApi } from "@/api/userApi";

export function useMyProfile() {
  return useQuery({
    queryKey: ["users", "me"],
    queryFn: userApi.getMyProfile,
    staleTime: 5 * 60_000,
  });
}
