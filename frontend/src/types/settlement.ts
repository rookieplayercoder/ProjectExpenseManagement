// Mirrors com.prateek.ProjectExpenseManagement.dto.SettlementSummaryResponse
export interface SettlementSummary {
  settlementId: string;
  paidByUserId: string;
  paidByName: string;
  paidToUserId: string;
  paidToName: string;
  amount: number;
  currencyCode: string;
  settlementDate: string;
  note: string | null;
}
