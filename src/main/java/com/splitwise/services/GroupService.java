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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final BalanceService balanceService;

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

    @Transactional(readOnly = true)
    public List<UserResponse> getGroupMembers(Long groupId, String requesterEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group members can view members list");
        }

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

    @Transactional
    public void leaveGroup(Long groupId, String requesterEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (group.getCreatedBy().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group creator cannot leave the group");
        }

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, requester.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not a member of this group");
        }

        Map<Long, BigDecimal> balances = balanceService.getGroupBalances(groupId);
        BigDecimal net = balances.getOrDefault(requester.getId(), BigDecimal.ZERO);
        if (net.compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Clear your outstanding balance before leaving the group"
            );
        }

        groupMemberRepository.deleteByGroup_IdAndUser_Id(groupId, requester.getId());
    }

    @Transactional
    public void removeMember(Long groupId, Long memberId, String requesterEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!group.getCreatedBy().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can remove members");
        }

        if (group.getCreatedBy().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group creator cannot be removed");
        }

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, memberId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in this group");
        }

        groupMemberRepository.deleteByGroup_IdAndUser_Id(groupId, memberId);
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