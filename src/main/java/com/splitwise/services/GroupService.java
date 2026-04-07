package com.splitwise.services;

import com.splitwise.dto.request.GroupRequest;
import com.splitwise.dto.response.GroupResponse;
import com.splitwise.dto.response.UserResponse;
import com.splitwise.models.Group;
import com.splitwise.models.GroupMember;
import com.splitwise.models.User;
import com.splitwise.models.enums.Role;
import com.splitwise.repositories.GroupMemberRepository;
import com.splitwise.repositories.GroupRepository;
import com.splitwise.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

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