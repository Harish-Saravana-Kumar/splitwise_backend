package com.splitwise.services;

import com.splitwise.dto.request.GroupInvitationRequest;
import com.splitwise.dto.response.GroupInvitationResponse;
import com.splitwise.dto.response.UserResponse;
import com.splitwise.models.Group;
import com.splitwise.models.GroupMember;
import com.splitwise.models.GroupMembershipRequest;
import com.splitwise.models.User;
import com.splitwise.models.enums.GroupMembershipRequestStatus;
import com.splitwise.models.enums.Role;
import com.splitwise.repositories.GroupMemberRepository;
import com.splitwise.repositories.GroupMembershipRequestRepository;
import com.splitwise.repositories.GroupRepository;
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
public class GroupInvitationService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMembershipRequestRepository requestRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupInvitationResponse sendInvitation(Long groupId, String inviterEmail, GroupInvitationRequest request) {
        User inviter = userRepository.findByEmail(inviterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inviter not found"));
        User invitedUser = userRepository.findByEmail(request.getInvitedEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invited user not found"));

        if (inviter.getId().equals(invitedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, inviter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group members can invite users");
        }

        if (groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, invitedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a member of this group");
        }

        boolean pendingExists = requestRepository.existsByGroup_IdAndInvitedUser_IdAndStatus(
                groupId,
                invitedUser.getId(),
                GroupMembershipRequestStatus.PENDING
        );
        if (pendingExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending invitation already exists for this user");
        }

        GroupMembershipRequest saved = requestRepository.save(GroupMembershipRequest.builder()
                .group(group)
                .invitedUser(invitedUser)
                .invitedBy(inviter)
                .status(GroupMembershipRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GroupInvitationResponse> getPendingInvitations(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return requestRepository.findByInvitedUser_IdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        GroupMembershipRequestStatus.PENDING
                ).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public GroupInvitationResponse acceptInvitation(Long requestId, String userEmail) {
        User invitedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        GroupMembershipRequest request = requestRepository.findByIdAndInvitedUser_Id(requestId, invitedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (request.getStatus() == GroupMembershipRequestStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation already rejected");
        }
        if (request.getStatus() == GroupMembershipRequestStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation already accepted");
        }

        if (groupMemberRepository.existsByGroup_IdAndUser_Id(request.getGroup().getId(), invitedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a group member");
        }

        request.setStatus(GroupMembershipRequestStatus.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);

        GroupMember member = GroupMember.builder()
                .group(request.getGroup())
                .user(invitedUser)
                .role(Role.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
        groupMemberRepository.save(member);

        return mapToResponse(request);
    }

    @Transactional
    public GroupInvitationResponse rejectInvitation(Long requestId, String userEmail) {
        User invitedUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        GroupMembershipRequest request = requestRepository.findByIdAndInvitedUser_Id(requestId, invitedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (request.getStatus() == GroupMembershipRequestStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation already accepted");
        }
        if (request.getStatus() == GroupMembershipRequestStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation already rejected");
        }

        request.setStatus(GroupMembershipRequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);

        return mapToResponse(request);
    }

    private GroupInvitationResponse mapToResponse(GroupMembershipRequest request) {
        return GroupInvitationResponse.builder()
                .id(request.getId())
                .groupId(request.getGroup().getId())
                .groupName(request.getGroup().getName())
                .invitedUser(mapToUserResponse(request.getInvitedUser()))
                .invitedBy(mapToUserResponse(request.getInvitedBy()))
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .respondedAt(request.getRespondedAt())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .profilePicture(user.getProfilePicture())
                .provider(user.getProvider())
                .build();
    }
}
