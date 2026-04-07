package com.splitwise.services;

import com.splitwise.dto.request.ExpenseRequest;
import com.splitwise.dto.request.ExpenseSplitRequest;
import com.splitwise.dto.response.ExpenseResponse;
import com.splitwise.dto.response.ExpenseSplitResponse;
import com.splitwise.dto.response.UserResponse;
import com.splitwise.models.Expense;
import com.splitwise.models.ExpenseSplit;
import com.splitwise.models.Group;
import com.splitwise.models.User;
import com.splitwise.models.enums.SplitType;
import com.splitwise.repositories.ExpenseRepository;
import com.splitwise.repositories.ExpenseSplitRepository;
import com.splitwise.repositories.GroupMemberRepository;
import com.splitwise.repositories.GroupRepository;
import com.splitwise.repositories.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExpenseResponse addExpense(ExpenseRequest request) {
        validateExpenseRequest(request);

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User paidBy = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), paidBy.getId())) {
            throw new RuntimeException("Payer is not a member of the group");
        }

        SplitType splitType;
        try {
            splitType = SplitType.valueOf(request.getSplitType().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid split type");
        }

        validateSplitTotals(request);
        validateSplitUsersBelongToGroup(group.getId(), request.getSplits());

        Expense expense = Expense.builder()
                .group(group)
                .paidBy(paidBy)
                .description(request.getDescription())
                .amount(request.getAmount())
                .splitType(splitType)
                .createdAt(LocalDateTime.now())
                .build();
        Expense savedExpense = expenseRepository.save(expense);

        List<ExpenseSplit> splits = request.getSplits().stream()
                .map(splitRequest -> buildExpenseSplit(savedExpense, splitRequest))
                .map(expenseSplitRepository::save)
                .toList();

        return mapToExpenseResponse(savedExpense, splits);
    }

    private void validateExpenseRequest(ExpenseRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }
        if (request.getSplits() == null || request.getSplits().isEmpty()) {
            throw new RuntimeException("At least one split is required");
        }
    }

    private void validateSplitTotals(ExpenseRequest request) {
        BigDecimal totalSplitAmount = request.getSplits().stream()
                .map(ExpenseSplitRequest::getOwedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSplitAmount.compareTo(request.getAmount()) != 0) {
            throw new RuntimeException("Sum of split amounts must equal total expense amount");
        }
    }

    private void validateSplitUsersBelongToGroup(Long groupId, List<ExpenseSplitRequest> splits) {
        Set<Long> splitUserIds = splits.stream()
                .map(ExpenseSplitRequest::getUserId)
                .collect(Collectors.toSet());

        for (Long splitUserId : splitUserIds) {
            if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, splitUserId)) {
                throw new RuntimeException("All split users must be members of the group");
            }
        }
    }

    public ExpenseResponse getExpenseById(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        List<ExpenseSplit> splits = expenseSplitRepository.findByExpense_Id(expenseId);
        return mapToExpenseResponse(expense, splits);
    }

    public List<ExpenseResponse> getExpensesByGroup(Long groupId) {
        return expenseRepository.findByGroup_IdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(expense -> {
                    List<ExpenseSplit> splits = expenseSplitRepository.findByExpense_Id(expense.getId());
                    return mapToExpenseResponse(expense, splits);
                })
                .toList();
    }

    private ExpenseSplit buildExpenseSplit(Expense expense, ExpenseSplitRequest splitRequest) {
        User user = userRepository.findById(splitRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ExpenseSplit.builder()
                .expense(expense)
                .user(user)
                .owedAmount(splitRequest.getOwedAmount())
                .settled(false)
                .build();
    }

    private ExpenseResponse mapToExpenseResponse(Expense expense, List<ExpenseSplit> splits) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .groupId(expense.getGroup().getId())
                .paidBy(mapToUserResponse(expense.getPaidBy()))
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .splitType(expense.getSplitType().name())
                .splits(splits.stream().map(this::mapToExpenseSplitResponse).toList())
                .createdAt(expense.getCreatedAt())
                .build();
    }

    private ExpenseSplitResponse mapToExpenseSplitResponse(ExpenseSplit split) {
        return ExpenseSplitResponse.builder()
                .id(split.getId())
                .user(mapToUserResponse(split.getUser()))
                .owedAmount(split.getOwedAmount())
                .settled(split.isSettled())
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