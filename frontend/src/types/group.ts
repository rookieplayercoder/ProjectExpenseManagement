// Mirrors com.prateek.ProjectExpenseManagement.dto.GroupSummaryResponse
export interface GroupSummary {
  groupId: string;
  groupName: string;
  description: string | null;
  createdByUserId: string;
  memberCount: number;
  createdAt: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.BalanceEntryResponse
export interface BalanceEntry {
  debtorUserId: string;
  debtorName: string;
  creditorUserId: string;
  creditorName: string;
  currencyCode: string;
  netAmount: number;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.GroupBalanceResponse
export interface GroupBalance {
  groupId: string;
  balances: BalanceEntry[];
}

// Mirrors com.prateek.ProjectExpenseManagement.domain.SplitType
export type SplitType = "EQUAL" | "EXACT" | "PERCENTAGE";

// Mirrors com.prateek.ProjectExpenseManagement.dto.GroupMemberResponse
export interface GroupMember {
  userId: string;
  fullName: string;
  email: string;
  joinedAt: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.GroupDetailResponse
export interface GroupDetail {
  groupId: string;
  groupName: string;
  description: string | null;
  createdByUserId: string;
  createdAt: string;
  members: GroupMember[];
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.UpdateGroupRequest
export interface UpdateGroupRequest {
  groupName: string;
  description?: string;
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.AddGroupMembersRequest
export interface AddGroupMembersRequest {
  userIds: string[];
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.CreateGroupRequest
export interface CreateGroupRequest {
  groupName: string;
  description?: string;
  createdByUserId: string;
  memberUserIds?: string[];
}

// Mirrors com.prateek.ProjectExpenseManagement.dto.CreateGroupResponse
export interface CreateGroupResponse {
  groupId: string;
  status: string;
  message: string;
}
