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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExpenseResponse addExpense(ExpenseRequest request, String requesterEmail) {
        SplitType splitType = parseSplitType(request.getSplitType());
        validateExpenseRequest(request, splitType);

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), requester.getId())) {
            throw new RuntimeException("Only group members can add expenses");
        }

        User paidBy = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), paidBy.getId())) {
            throw new RuntimeException("Payer is not a member of the group");
        }

        List<ExpenseSplitRequest> normalizedSplits = normalizeSplits(splitType, request, group.getId());

        validateSplitTotals(request.getAmount(), normalizedSplits);
        validateNoDuplicateSplitUsers(normalizedSplits);
        validateSplitUsersBelongToGroup(group.getId(), normalizedSplits);

        Expense expense = Expense.builder()
                .group(group)
                .paidBy(paidBy)
                .description(request.getDescription())
                .amount(request.getAmount())
                .splitType(splitType)
                .createdAt(LocalDateTime.now())
                .build();
        Expense savedExpense = expenseRepository.save(expense);

        List<ExpenseSplit> splits = normalizedSplits.stream()
                .map(splitRequest -> buildExpenseSplit(savedExpense, splitRequest))
                .map(expenseSplitRepository::save)
                .toList();

        return mapToExpenseResponse(savedExpense, splits);
    }

    private SplitType parseSplitType(String value) {
        try {
            return SplitType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid split type");
        }
    }

    private void validateExpenseRequest(ExpenseRequest request, SplitType splitType) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }
        if (splitType != SplitType.EQUAL && (request.getSplits() == null || request.getSplits().isEmpty())) {
            throw new RuntimeException("At least one split is required");
        }
    }

    private List<ExpenseSplitRequest> normalizeSplits(SplitType splitType, ExpenseRequest request, Long groupId) {
        return switch (splitType) {
            case EQUAL -> buildEqualSplits(groupId, request.getAmount());
            case EXACT -> request.getSplits();
            case PERCENTAGE -> buildPercentageSplits(request.getAmount(), request.getSplits());
            case SHARES -> buildShareSplits(request.getAmount(), request.getSplits());
        };
    }

    private void validateSplitTotals(BigDecimal amount, List<ExpenseSplitRequest> splits) {
        BigDecimal totalSplitAmount = splits.stream()
                .map(ExpenseSplitRequest::getOwedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSplitAmount.compareTo(amount) != 0) {
            throw new RuntimeException("Sum of split amounts must equal total expense amount");
        }
    }

    private List<ExpenseSplitRequest> buildEqualSplits(Long groupId, BigDecimal amount) {
        List<Long> memberIds = groupMemberRepository.findByGroup_Id(groupId)
                .stream()
                .map(groupMember -> groupMember.getUser().getId())
                .distinct()
                .sorted()
                .toList();

        if (memberIds.isEmpty()) {
            throw new RuntimeException("Group has no members to split equally");
        }

        BigDecimal memberCount = BigDecimal.valueOf(memberIds.size());
        BigDecimal baseShare = amount.divide(memberCount, 2, RoundingMode.DOWN);
        BigDecimal remainder = amount.subtract(baseShare.multiply(memberCount));
        int extraPaiseCount = remainder.movePointRight(2).intValueExact();

        List<ExpenseSplitRequest> equalSplits = new ArrayList<>();
        for (int index = 0; index < memberIds.size(); index++) {
            BigDecimal share = baseShare;
            if (index < extraPaiseCount) {
                share = share.add(new BigDecimal("0.01"));
            }

            equalSplits.add(ExpenseSplitRequest.builder()
                    .userId(memberIds.get(index))
                    .owedAmount(share)
                    .build());
        }

        return equalSplits;
    }

    private List<ExpenseSplitRequest> buildPercentageSplits(BigDecimal amount, List<ExpenseSplitRequest> splits) {
        validateWeightRows(splits, "Percentage");

        BigDecimal totalPercentage = splits.stream()
                .map(ExpenseSplitRequest::getOwedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercentage.compareTo(new BigDecimal("100")) != 0) {
            throw new RuntimeException("For PERCENTAGE split, total percentage must be exactly 100");
        }

        return allocateByWeights(amount, splits, totalPercentage);
    }

    private List<ExpenseSplitRequest> buildShareSplits(BigDecimal amount, List<ExpenseSplitRequest> splits) {
        validateWeightRows(splits, "Share");

        BigDecimal totalShares = splits.stream()
                .map(ExpenseSplitRequest::getOwedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalShares.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("For SHARES split, total shares must be greater than zero");
        }

        return allocateByWeights(amount, splits, totalShares);
    }

    private void validateWeightRows(List<ExpenseSplitRequest> splits, String label) {
        boolean hasInvalid = splits.stream()
                .anyMatch(split -> split.getOwedAmount() == null || split.getOwedAmount().compareTo(BigDecimal.ZERO) <= 0);
        if (hasInvalid) {
            throw new RuntimeException(label + " value must be greater than zero for each split row");
        }
    }

    private List<ExpenseSplitRequest> allocateByWeights(
            BigDecimal amount,
            List<ExpenseSplitRequest> weightedSplits,
            BigDecimal totalWeight
    ) {
        long totalPaise = amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        BigDecimal totalPaiseDecimal = BigDecimal.valueOf(totalPaise);

        List<WeightedAllocation> allocations = new ArrayList<>();
        long assignedPaise = 0L;

        for (ExpenseSplitRequest split : weightedSplits) {
            BigDecimal rawPaise = totalPaiseDecimal
                    .multiply(split.getOwedAmount())
                    .divide(totalWeight, 10, RoundingMode.HALF_UP);

            long basePaise = rawPaise.setScale(0, RoundingMode.DOWN).longValueExact();
            BigDecimal fractionalPart = rawPaise.subtract(BigDecimal.valueOf(basePaise));

            allocations.add(new WeightedAllocation(split.getUserId(), basePaise, fractionalPart));
            assignedPaise += basePaise;
        }

        long remainingPaise = totalPaise - assignedPaise;
        allocations.sort(Comparator
                .comparing(WeightedAllocation::fractionalPart)
                .reversed()
                .thenComparing(WeightedAllocation::userId));

        for (int index = 0; index < remainingPaise; index++) {
            allocations.get(index % allocations.size()).addPaise(1);
        }

        return allocations.stream()
                .map(entry -> ExpenseSplitRequest.builder()
                        .userId(entry.userId())
                        .owedAmount(BigDecimal.valueOf(entry.allocatedPaise()).movePointLeft(2))
                        .build())
                .toList();
    }

    private void validateNoDuplicateSplitUsers(List<ExpenseSplitRequest> splits) {
        Set<Long> unique = splits.stream().map(ExpenseSplitRequest::getUserId).collect(Collectors.toSet());
        if (unique.size() != splits.size()) {
            throw new RuntimeException("Duplicate users are not allowed in split rows");
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

    @Transactional
    public void deleteExpense(Long expenseId, String requesterEmail) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isExpenseCreator = expense.getPaidBy().getId().equals(requester.getId());
        boolean isGroupCreator = expense.getGroup().getCreatedBy().getId().equals(requester.getId());

        if (!isExpenseCreator && !isGroupCreator) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only expense creator or group creator can delete this expense"
            );
        }

        expenseSplitRepository.deleteByExpense_Id(expense.getId());
        expenseRepository.delete(expense);
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

    private static class WeightedAllocation {
        private final Long userId;
        private long allocatedPaise;
        private final BigDecimal fractionalPart;

        private WeightedAllocation(Long userId, long allocatedPaise, BigDecimal fractionalPart) {
            this.userId = userId;
            this.allocatedPaise = allocatedPaise;
            this.fractionalPart = fractionalPart;
        }

        public Long userId() {
            return userId;
        }

        public long allocatedPaise() {
            return allocatedPaise;
        }

        public BigDecimal fractionalPart() {
            return fractionalPart;
        }

        public void addPaise(long increment) {
            this.allocatedPaise += increment;
        }
    }
}