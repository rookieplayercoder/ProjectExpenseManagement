import type { SplitType } from "@/types/group";

// Mirrors com.prateek.ProjectExpenseManagement.dto.ExpenseSummaryResponse
export interface ExpenseSummary {
  expenseId: string;
  title: string;
  totalAmount: number;
  currencyCode: string;
  splitType: SplitType;
  paidByUserId: string;
  paidByName: string;
  expenseDate: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.ExpenseParticipantResponse
export interface ExpenseParticipant {
  userId: string;
  userName: string;
  owedAmount: number;
  percentageValue: number | null;
  exactAmountInput: number | null;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.ExpenseDetailResponse
export interface ExpenseDetail {
  expenseId: string;
  groupId: string | null;
  paidByUserId: string;
  paidByName: string;
  title: string;
  description: string | null;
  totalAmount: number;
  currencyCode: string;
  splitType: SplitType;
  expenseDate: string;
  createdByUserId: string;
  createdAt: string;
  participants: ExpenseParticipant[];
}
