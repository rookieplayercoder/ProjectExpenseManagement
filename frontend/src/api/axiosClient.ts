import axios, { type InternalAxiosRequestConfig } from "axios";
import { authStorage } from "@/utils/authStorage";
import type { ApiErrorResponse } from "@/types/api";

export const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1",
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach the JWT (if present) to every outgoing request.
axiosClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = authStorage.getToken();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Normalize backend errors (ApiErrorResponse) and force a re-login on 401.
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError<ApiErrorResponse>(error)) {
      if (error.response?.status === 401) {
        authStorage.clear();
        if (window.location.pathname !== "/login") {
          window.location.href = "/login";
        }
      }
      const message =
        error.response?.data?.message ?? error.message ?? "Something went wrong. Please try again.";
      return Promise.reject(new Error(message));
    }
    return Promise.reject(error);
  },
);
