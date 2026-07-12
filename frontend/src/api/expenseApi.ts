import { axiosClient } from "@/api/axiosClient";
import type { SplitType } from "@/types/group";
import type { ExpenseDetail } from "@/types/expense";

// Mirrors com.prateek.ProjectExpenseManagement.dto.ParticipantShareRequest
export interface ParticipantShareRequest {
  userId: string;
  exactAmount?: number;
  percentage?: number;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.CreateExpenseRequest
export interface CreateExpenseRequest {
  groupId?: string;
  paidByUserId: string;
  title: string;
  description?: string;
  totalAmount: number;
  currencyCode: string;
  splitType: SplitType;
  expenseDate: string;
  createdByUserId: string;
  participants: ParticipantShareRequest[];
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.CreateExpenseResponse
export interface CreateExpenseResponse {
  expenseId: string;
  status: string;
  message: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.UpdateExpenseRequest
export interface UpdateExpenseRequest {
  paidByUserId: string;
  title: string;
  description?: string;
  totalAmount: number;
  currencyCode: string;
  splitType: SplitType;
  expenseDate: string;
  participants: ParticipantShareRequest[];
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.UpdateExpenseResponse
export interface UpdateExpenseResponse {
  expenseId: string;
  status: string;
  message: string;
}

export const expenseApi = {
  // POST /api/v1/expenses
  createExpense: async (
    payload: CreateExpenseRequest,
    idempotencyKey?: string,
  ): Promise<CreateExpenseResponse> => {
    const { data } = await axiosClient.post<CreateExpenseResponse>("/expenses", payload, {
      headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
    });
    return data;
  },

  // GET /api/v1/expenses/{expenseId}
  getExpense: async (expenseId: string): Promise<ExpenseDetail> => {
    const { data } = await axiosClient.get<ExpenseDetail>(`/expenses/${expenseId}`);
    return data;
  },

  // PUT /api/v1/expenses/{expenseId}
  updateExpense: async (
    expenseId: string,
    payload: UpdateExpenseRequest,
  ): Promise<UpdateExpenseResponse> => {
    const { data } = await axiosClient.put<UpdateExpenseResponse>(`/expenses/${expenseId}`, payload);
    return data;
  },

  // DELETE /api/v1/expenses/{expenseId}
  deleteExpense: async (expenseId: string): Promise<void> => {
    await axiosClient.delete(`/expenses/${expenseId}`);
  },
};
