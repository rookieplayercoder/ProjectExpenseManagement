import { axiosClient } from "@/api/axiosClient";

// Mirrors com.prateek.ProjectExpenseManagement.dto.SettleBalanceRequest
export interface SettleBalanceRequest {
  groupId?: string;
  paidByUserId: string;
  paidToUserId: string;
  amount: number;
  currencyCode: string;
  settlementDate: string;
  note?: string;
  createdByUserId: string;
}

export interface SettleBalanceResponse {
  settlementId: string;
  status: string;
  message: string;
}

export const settlementApi = {
  // POST /api/v1/settlements
  settleBalance: async (
    payload: SettleBalanceRequest,
    idempotencyKey?: string,
  ): Promise<SettleBalanceResponse> => {
    const { data } = await axiosClient.post<SettleBalanceResponse>("/settlements", payload, {
      headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
    });
    return data;
  },
};
