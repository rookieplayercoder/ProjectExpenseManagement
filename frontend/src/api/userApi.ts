import { axiosClient } from "@/api/axiosClient";
import type { CreateUserRequest, CreateUserResponse, UserLookup, UserProfile } from "@/types/auth";

export const userApi = {
  // POST /api/v1/users (public - registration)
  register: async (payload: CreateUserRequest): Promise<CreateUserResponse> => {
    const { data } = await axiosClient.post<CreateUserResponse>("/users", payload);
    return data;
  },

  // GET /api/v1/users/me
  getMyProfile: async (): Promise<UserProfile> => {
    const { data } = await axiosClient.get<UserProfile>("/users/me");
    return data;
  },

  // GET /api/v1/users/lookup?email=...  - used by "add member" flows
  lookupByEmail: async (email: string): Promise<UserLookup> => {
    const { data } = await axiosClient.get<UserLookup>("/users/lookup", { params: { email } });
    return data;
  },
};
