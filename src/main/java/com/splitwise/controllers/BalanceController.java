package com.splitwise.controllers;

import com.splitwise.dto.response.SettlementBalanceResponse;
import com.splitwise.services.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/group/{groupId}/pending")
    public ResponseEntity<SettlementBalanceResponse> getPendingSettlementBetween(
            @PathVariable Long groupId,
            @RequestParam Long payerId,
            @RequestParam Long receiverId
    ) {
        return ResponseEntity.ok(
                SettlementBalanceResponse.builder()
                        .groupId(groupId)
                        .payerId(payerId)
                        .receiverId(receiverId)
                        .amount(balanceService.getPendingSettlementBetween(groupId, payerId, receiverId))
                        .build()
        );
    }

    @GetMapping("/group/{groupId}/settlements")
    public ResponseEntity<?> getMinimumSettlements(@PathVariable Long groupId) {
        return new ResponseEntity<>(balanceService.getMinimumSettlements(groupId), HttpStatus.OK);
    }
}