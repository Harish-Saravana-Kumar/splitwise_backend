package com.splitwise.controllers;

import com.splitwise.dto.request.SettlementRequest;
import com.splitwise.services.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<?> settleUp(@RequestBody @Valid SettlementRequest request) {
        return new ResponseEntity<>(settlementService.settleUp(request), HttpStatus.CREATED);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getSettlementsByGroup(@PathVariable Long groupId) {
        return new ResponseEntity<>(settlementService.getSettlementsByGroup(groupId), HttpStatus.OK);
    }
}