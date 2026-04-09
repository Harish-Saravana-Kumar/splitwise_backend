package com.splitwise.assistant.service;

import com.splitwise.models.Expense;
import com.splitwise.models.GroupMember;
import com.splitwise.repositories.ExpenseRepository;
import com.splitwise.repositories.GroupMemberRepository;
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
    public List<Expense> getRecentExpensesForUser(Long userId, int limit) {
        List<GroupMember> memberships = groupMemberRepository.findByUser_Id(userId);
        Set<Long> groupIds = new HashSet<>();
        for (GroupMember membership : memberships) {
            groupIds.add(membership.getGroup().getId());
        }

        List<Expense> all = new ArrayList<>();
        for (Long groupId : groupIds) {
            all.addAll(expenseRepository.findByGroup_IdOrderByCreatedAtDesc(groupId));
        }

        all.sort(Comparator.comparing(Expense::getCreatedAt).reversed());
        if (all.size() <= limit) {
            return all;
        }
        return all.subList(0, limit);
    }
}
