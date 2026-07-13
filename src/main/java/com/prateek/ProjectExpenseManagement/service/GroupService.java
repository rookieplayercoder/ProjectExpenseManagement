package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.dto.AddGroupMembersRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateGroupRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateGroupResponse;
import com.prateek.ProjectExpenseManagement.dto.GroupDetailResponse;
import com.prateek.ProjectExpenseManagement.dto.GroupSummaryResponse;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import com.prateek.ProjectExpenseManagement.repository.GroupRepository;
import com.prateek.ProjectExpenseManagement.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public CreateGroupResponse createGroup(@Valid CreateGroupRequest request) {
        // Creator is always a member; LinkedHashSet dedupes if they're also listed explicitly
        // while keeping insertion order stable for the batch insert below.
        LinkedHashSet<UUID> memberIds = new LinkedHashSet<>();
        memberIds.add(request.getCreatedByUserId());
        if (request.getMemberUserIds() != null) {
            memberIds.addAll(request.getMemberUserIds());
        }

        userRepository.assertUsersExist(List.copyOf(memberIds));

        UUID groupId = groupRepository.insertGroup(request);
        groupRepository.addMembers(groupId, List.copyOf(memberIds));

        return new CreateGroupResponse(groupId, "SUCCESS", "Group created successfully");
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
    public List<GroupSummaryResponse> getGroupsForUser(UUID userId) {
        return groupRepository.findGroupsForUser(userId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
    public GroupDetailResponse getGroupDetail(UUID groupId, UUID callerUserId) {
        // findGroupDetail throws ResourceNotFoundException (404) if the group doesn't
        // exist at all. assertUsersBelongToGroup then throws BusinessValidationException
        // (400) if it exists but the caller isn't a member. Deliberately checking
        // existence first: matches the frontend's own error copy ("may not exist, or
        // you may not have access"), and is consistent with how every other
        // group-scoped write endpoint in this codebase already orders its checks.
        GroupDetailResponse detail = groupRepository.findGroupDetail(groupId);
        groupRepository.assertUsersBelongToGroup(groupId, List.of(callerUserId));
        return detail;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public GroupDetailResponse addMembersToGroup(UUID groupId, UUID callerUserId, @Valid AddGroupMembersRequest request) {
        groupRepository.assertGroupExists(groupId);
        groupRepository.assertUsersBelongToGroup(groupId, List.of(callerUserId));

        List<UUID> newMemberIds = List.copyOf(new LinkedHashSet<>(request.getUserIds()));
        userRepository.assertUsersExist(newMemberIds);

        groupRepository.addMembers(groupId, newMemberIds);
        return groupRepository.findGroupDetail(groupId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public GroupDetailResponse removeMemberFromGroup(UUID groupId, UUID callerUserId, UUID userIdToRemove) {
        groupRepository.assertGroupExists(groupId);
        groupRepository.assertUsersBelongToGroup(groupId, List.of(callerUserId));

        // A member with an unsettled balance can't be removed - doing so would strand
        // real, unresolved debt in user_balance with no active member on one end of it.
        // They (or whoever they owe) need to settle up first.
        if (groupRepository.hasOutstandingBalance(groupId, userIdToRemove)) {
            throw new BusinessValidationException(
                    "Cannot remove this member: they have an outstanding balance in this group. Settle up first.");
        }

        groupRepository.removeMember(groupId, userIdToRemove);
        return groupRepository.findGroupDetail(groupId);
    }
}
