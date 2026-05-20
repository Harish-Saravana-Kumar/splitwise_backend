package com.splitwise.controllers;

import com.splitwise.dto.response.ApiResponse;
import com.splitwise.services.GroupService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupById(@PathVariable Long groupId) {
        return new ResponseEntity<>(groupService.getGroupById(groupId), HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getGroupsByUser(@PathVariable Long userId) {
        return new ResponseEntity<>(groupService.getGroupsByUser(userId), HttpStatus.OK);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Long groupId, Authentication authentication) {
        return new ResponseEntity<>(
                ApiResponse.success("Group members retrieved", groupService.getGroupMembers(groupId, authentication.getName())),
                HttpStatus.OK
        );
    }

        @PostMapping("/{groupId}/leave")
        public ResponseEntity<?> leaveGroup(@PathVariable Long groupId, Authentication authentication) {
        groupService.leaveGroup(groupId, authentication.getName());
        return new ResponseEntity<>(
            ApiResponse.success("Left the group", null),
            HttpStatus.OK
        );
        }

        @DeleteMapping("/{groupId}/members/{memberId}")
        public ResponseEntity<?> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            Authentication authentication
        ) {
        groupService.removeMember(groupId, memberId, authentication.getName());
        return new ResponseEntity<>(
            ApiResponse.success("Member removed", null),
            HttpStatus.OK
        );
        }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId, @RequestParam Long requesterUserId) {
        groupService.deleteGroup(groupId, requesterUserId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}