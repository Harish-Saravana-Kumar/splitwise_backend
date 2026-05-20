package com.splitwise.assistant.service;

import com.splitwise.assistant.dto.ExpenseSummary;
import com.splitwise.models.Expense;
import com.splitwise.models.GroupMember;
import com.splitwise.repositories.ExpenseRepository;
import com.splitwise.repositories.GroupMemberRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantQueryService {

    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<ExpenseSummary> getRecentExpenseSummariesForUser(Long userId, int limit) {
        List<Long> groupIds = findGroupIdsForUser(userId);
        if (groupIds.isEmpty()) {
            return List.of();
        }

        List<Expense> all = expenseRepository.findByGroup_IdInOrderByCreatedAtDesc(groupIds);
        all.sort(Comparator.comparing(Expense::getCreatedAt).reversed());

        List<Expense> limited = all.size() <= limit ? all : all.subList(0, limit);
        return limited.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseSummary> getExpenseSummariesForUserInRange(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Long> groupIds = findGroupIdsForUser(userId);
        if (groupIds.isEmpty()) {
            return List.of();
        }

        List<Expense> expenses = expenseRepository.findByGroup_IdInAndCreatedAtBetweenOrderByCreatedAtDesc(
                groupIds,
                start,
                end
        );
        return expenses.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSpentForUserInRange(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Long> groupIds = findGroupIdsForUser(userId);
        if (groupIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<Expense> expenses = expenseRepository.findByGroup_IdInAndCreatedAtBetweenOrderByCreatedAtDesc(
                groupIds,
                start,
                end
        );
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Long> findGroupIdsForUser(Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUser_Id(userId);
        Set<Long> groupIds = new HashSet<>();
        for (GroupMember membership : memberships) {
            groupIds.add(membership.getGroup().getId());
        }
        return new ArrayList<>(groupIds);
    }

    private ExpenseSummary toSummary(Expense expense) {
        return new ExpenseSummary(
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getName(),
                expense.getGroup().getName(),
                expense.getCreatedAt()
        );
    }
}
