package com.splitwise.services;

import com.splitwise.dto.request.GroupRequest;
import com.splitwise.dto.response.GroupResponse;
import com.splitwise.dto.response.UserResponse;
import com.splitwise.models.Group;
import com.splitwise.models.GroupMember;
import com.splitwise.models.User;
import com.splitwise.models.enums.Role;
import com.splitwise.repositories.ExpenseRepository;
import com.splitwise.repositories.ExpenseSplitRepository;
import com.splitwise.repositories.GroupMemberRepository;
import com.splitwise.repositories.GroupRepository;
import com.splitwise.repositories.SettlementRepository;
import com.splitwise.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
        private final ExpenseRepository expenseRepository;
        private final ExpenseSplitRepository expenseSplitRepository;
        private final SettlementRepository settlementRepository;

    public GroupResponse createGroup(GroupRequest request, Long creatorUserId) {
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .build();
        Group savedGroup = groupRepository.save(group);

        GroupMember creatorMembership = GroupMember.builder()
                .group(savedGroup)
                .user(creator)
                .role(Role.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();
        groupMemberRepository.save(creatorMembership);

        return mapToGroupResponse(savedGroup);
    }

    public GroupResponse getGroupById(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return mapToGroupResponse(group);
    }

    public List<GroupResponse> getGroupsByUser(Long userId) {
        return groupMemberRepository.findByUser_Id(userId)
                .stream()
                .map(GroupMember::getGroup)
                .map(this::mapToGroupResponse)
                .toList();
    }

    public GroupResponse addMemberToGroup(Long groupId, Long userId) {
        if (groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
            throw new RuntimeException("User already in group");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .role(Role.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
        groupMemberRepository.save(member);

        return mapToGroupResponse(group);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getGroupMembers(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return groupMemberRepository.findByGroup_Id(group.getId())
                .stream()
                .map(GroupMember::getUser)
                .map(this::mapToUserResponse)
                .toList();
    }

    @Transactional
    public void deleteGroup(Long groupId, Long requesterUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (!group.getCreatedBy().getId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can delete this group");
        }

        expenseSplitRepository.deleteByExpense_Group_Id(groupId);
        settlementRepository.deleteByGroup_Id(groupId);
        groupMemberRepository.deleteByGroup_Id(groupId);
        expenseRepository.deleteByGroup_Id(groupId);
        groupRepository.delete(group);
    }

    private GroupResponse mapToGroupResponse(Group group) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(mapToUserResponse(group.getCreatedBy()))
                .createdAt(group.getCreatedAt())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}