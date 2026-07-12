// Mirrors com.prateek.ProjectExpenseManagement.dto.LoginRequest
export interface LoginRequest {
  email: string;
  password: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.LoginResponse
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  userId: string;
  email: string;
  role: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.CreateUserRequest
export interface CreateUserRequest {
  email: string;
  fullName: string;
  mobileNumber?: string;
  password: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.CreateUserResponse
export interface CreateUserResponse {
  userId: string;
  status: string;
  message: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.AuthUser (session, not backend DTO)
export interface AuthUser {
  userId: string;
  email: string;
  role: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.UserProfileResponse
export interface UserProfile {
  userId: string;
  email: string;
  fullName: string;
  mobileNumber: string | null;
  role: string;
  createdAt: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.UserLookupResponse
export interface UserLookup {
  userId: string;
  fullName: string;
  email: string;
}
