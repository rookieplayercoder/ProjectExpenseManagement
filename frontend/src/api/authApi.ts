import { axiosClient } from "@/api/axiosClient";
import type { LoginRequest, LoginResponse } from "@/types/auth";

export const authApi = {
  // POST /api/v1/auth/login
  login: async (payload: LoginRequest): Promise<LoginResponse> => {
    const { data } = await axiosClient.post<LoginResponse>("/auth/login", payload);
    return data;
  },
};
