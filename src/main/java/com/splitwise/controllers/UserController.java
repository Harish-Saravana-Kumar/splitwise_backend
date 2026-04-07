package com.splitwise.controllers;

import com.splitwise.dto.request.GroupRequest;
import com.splitwise.services.GroupService;
import com.splitwise.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final GroupService groupService;

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return new ResponseEntity<>(userService.getUserById(id), HttpStatus.OK);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        return new ResponseEntity<>(userService.getUserByEmail(email), HttpStatus.OK);
    }

    @GetMapping("/groups")
    public ResponseEntity<?> getGroupsByUser(@RequestParam Long userId) {
        return new ResponseEntity<>(groupService.getGroupsByUser(userId), HttpStatus.OK);
    }

    @PostMapping("/groups")
    public ResponseEntity<?> createGroupForUser(@RequestBody @Valid GroupRequest request,
                                                @RequestParam Long creatorUserId) {
        return new ResponseEntity<>(groupService.createGroup(request, creatorUserId), HttpStatus.CREATED);
    }
}