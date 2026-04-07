package com.splitwise.controllers;

import com.splitwise.services.GroupService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMemberToGroup(@PathVariable Long groupId, @RequestParam Long userId) {
        return new ResponseEntity<>(groupService.addMemberToGroup(groupId, userId), HttpStatus.OK);
    }
}