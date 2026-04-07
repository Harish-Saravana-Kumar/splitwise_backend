package com.splitwise.controllers;

import com.splitwise.dto.request.ExpenseRequest;
import com.splitwise.services.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<?> addExpense(@RequestBody @Valid ExpenseRequest request, Authentication authentication) {
        return new ResponseEntity<>(expenseService.addExpense(request, authentication.getName()), HttpStatus.CREATED);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<?> getExpenseById(@PathVariable Long expenseId) {
        return new ResponseEntity<>(expenseService.getExpenseById(expenseId), HttpStatus.OK);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getExpensesByGroup(@PathVariable Long groupId) {
        return new ResponseEntity<>(expenseService.getExpensesByGroup(groupId), HttpStatus.OK);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long expenseId, Authentication authentication) {
        expenseService.deleteExpense(expenseId, authentication.getName());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}