package com.splitwise.controllers;

import com.splitwise.dto.request.GroupInvitationRequest;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.services.GroupInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupInvitationController {

    private final GroupInvitationService groupInvitationService;

    @PostMapping("/{groupId}/invite")
    public ResponseEntity<?> inviteUser(
            @PathVariable Long groupId,
            @RequestBody @Valid GroupInvitationRequest request,
            Authentication authentication
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Invitation sent",
                        groupInvitationService.sendInvitation(groupId, authentication.getName(), request)
                ),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/invitations")
    public ResponseEntity<?> getInvitations(Authentication authentication) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Pending invitations retrieved",
                        groupInvitationService.getPendingInvitations(authentication.getName())
                ),
                HttpStatus.OK
        );
    }

    @PostMapping("/invitations/{requestId}/accept")
    public ResponseEntity<?> acceptInvitation(
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Invitation accepted",
                        groupInvitationService.acceptInvitation(requestId, authentication.getName())
                ),
                HttpStatus.OK
        );
    }

    @PostMapping("/invitations/{requestId}/reject")
    public ResponseEntity<?> rejectInvitation(
            @PathVariable Long requestId,
            Authentication authentication
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Invitation rejected",
                        groupInvitationService.rejectInvitation(requestId, authentication.getName())
                ),
                HttpStatus.OK
        );
    }
}
