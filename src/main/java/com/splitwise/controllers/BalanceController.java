package com.splitwise.controllers;

import com.splitwise.services.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getGroupBalances(@PathVariable Long groupId) {
        return new ResponseEntity<>(balanceService.getGroupBalances(groupId), HttpStatus.OK);
    }

    @GetMapping("/group/{groupId}/settlements")
    public ResponseEntity<?> getMinimumSettlements(@PathVariable Long groupId) {
        return new ResponseEntity<>(balanceService.getMinimumSettlements(groupId), HttpStatus.OK);
    }
}