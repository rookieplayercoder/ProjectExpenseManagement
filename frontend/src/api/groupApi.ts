import { axiosClient } from "@/api/axiosClient";
import type {
  AddGroupMembersRequest,
  CreateGroupRequest,
  CreateGroupResponse,
  GroupBalance,
  GroupDetail,
  GroupSummary,
  UpdateGroupRequest,
} from "@/types/group";
import type { ExpenseSummary } from "@/types/expense";
import type { SettlementSummary } from "@/types/settlement";

export const groupApi = {
  // GET /api/v1/groups - groups the authenticated user belongs to
  listMyGroups: async (): Promise<GroupSummary[]> => {
    const { data } = await axiosClient.get<GroupSummary[]>("/groups");
    return data;
  },

  // POST /api/v1/groups
  createGroup: async (payload: CreateGroupRequest): Promise<CreateGroupResponse> => {
    const { data } = await axiosClient.post<CreateGroupResponse>("/groups", payload);
    return data;
  },

  // GET /api/v1/groups/{groupId}
  getGroupDetail: async (groupId: string): Promise<GroupDetail> => {
    const { data } = await axiosClient.get<GroupDetail>(`/groups/${groupId}`);
    return data;
  },

  // PUT /api/v1/groups/{groupId}
  updateGroup: async (groupId: string, payload: UpdateGroupRequest): Promise<GroupDetail> => {
    const { data } = await axiosClient.put<GroupDetail>(`/groups/${groupId}`, payload);
    return data;
  },

  // POST /api/v1/groups/{groupId}/members
  addMembers: async (groupId: string, payload: AddGroupMembersRequest): Promise<GroupDetail> => {
    const { data } = await axiosClient.post<GroupDetail>(`/groups/${groupId}/members`, payload);
    return data;
  },

  // DELETE /api/v1/groups/{groupId}/members/{userId}
  removeMember: async (groupId: string, userId: string): Promise<GroupDetail> => {
    const { data } = await axiosClient.delete<GroupDetail>(`/groups/${groupId}/members/${userId}`);
    return data;
  },

  // GET /api/v1/groups/{groupId}/balances
  getGroupBalances: async (groupId: string): Promise<GroupBalance> => {
    const { data } = await axiosClient.get<GroupBalance>(`/groups/${groupId}/balances`);
    return data;
  },

  // GET /api/v1/groups/{groupId}/expenses
  getGroupExpenses: async (groupId: string): Promise<ExpenseSummary[]> => {
    const { data } = await axiosClient.get<ExpenseSummary[]>(`/groups/${groupId}/expenses`);
    return data;
  },

  // GET /api/v1/groups/{groupId}/settlements
  getGroupSettlements: async (groupId: string): Promise<SettlementSummary[]> => {
    const { data } = await axiosClient.get<SettlementSummary[]>(`/groups/${groupId}/settlements`);
    return data;
  },
};
