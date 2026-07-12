package com.prateek.ProjectExpenseManagement.service;

import com.prateek.ProjectExpenseManagement.dto.CreateGroupRequest;
import com.prateek.ProjectExpenseManagement.dto.CreateGroupResponse;
import com.prateek.ProjectExpenseManagement.dto.GroupSummaryResponse;
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
}
