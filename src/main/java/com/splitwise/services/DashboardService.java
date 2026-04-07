package com.splitwise.services;

import com.splitwise.dto.response.DashboardGroupSummaryResponse;
import com.splitwise.dto.response.DashboardPersonBalanceResponse;
import com.splitwise.dto.response.DashboardResponse;
import com.splitwise.models.Expense;
import com.splitwise.models.ExpenseSplit;
import com.splitwise.models.Group;
import com.splitwise.models.GroupMember;
import com.splitwise.models.User;
import com.splitwise.repositories.ExpenseRepository;
import com.splitwise.repositories.ExpenseSplitRepository;
import com.splitwise.repositories.GroupMemberRepository;
import com.splitwise.repositories.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardByUserEmail(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<GroupMember> memberships = groupMemberRepository.findByUser_Id(currentUser.getId());
        List<DashboardGroupSummaryResponse> groupSummaries = new ArrayList<>();
        Map<Long, BigDecimal> personNetMap = new HashMap<>();

        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalOwes = BigDecimal.ZERO;

        for (GroupMember membership : memberships) {
            Group group = membership.getGroup();
            Long groupId = group.getId();

            List<Expense> expenses = expenseRepository.findByGroup_Id(groupId);
            BigDecimal groupTotalExpense = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<ExpenseSplit> splits = expenseSplitRepository.findByExpense_Group_Id(groupId);
            BigDecimal userNetInGroup = BigDecimal.ZERO;

            for (ExpenseSplit split : splits) {
                if (split.isSettled()) {
                    continue;
                }

                Long owingUserId = split.getUser().getId();
                Long paidByUserId = split.getExpense().getPaidBy().getId();
                BigDecimal amount = split.getOwedAmount();

                if (currentUser.getId().equals(owingUserId)) {
                    userNetInGroup = userNetInGroup.subtract(amount);
                }
                if (currentUser.getId().equals(paidByUserId)) {
                    userNetInGroup = userNetInGroup.add(amount);
                }

                // Person-to-person net: + means other user owes current user; - means current user owes other user.
                if (currentUser.getId().equals(owingUserId) && !currentUser.getId().equals(paidByUserId)) {
                    personNetMap.merge(paidByUserId, amount.negate(), BigDecimal::add);
                }
                if (currentUser.getId().equals(paidByUserId) && !currentUser.getId().equals(owingUserId)) {
                    personNetMap.merge(owingUserId, amount, BigDecimal::add);
                }
            }

            if (userNetInGroup.compareTo(BigDecimal.ZERO) > 0) {
                totalPaid = totalPaid.add(userNetInGroup);
            } else if (userNetInGroup.compareTo(BigDecimal.ZERO) < 0) {
                totalOwes = totalOwes.add(userNetInGroup.abs());
            }

            groupSummaries.add(DashboardGroupSummaryResponse.builder()
                    .groupId(groupId)
                    .groupName(group.getName())
                    .totalExpense(groupTotalExpense)
                    .userNetBalance(userNetInGroup)
                    .build());
        }

        List<DashboardPersonBalanceResponse> personBalances = personNetMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) != 0)
                .sorted((left, right) -> right.getValue().abs().compareTo(left.getValue().abs()))
                .map(entry -> {
                    User user = userRepository.findById(entry.getKey())
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return DashboardPersonBalanceResponse.builder()
                            .userId(user.getId())
                            .userName(user.getName())
                            .netAmount(entry.getValue())
                            .build();
                })
                .toList();

        groupSummaries.sort(Comparator.comparing(DashboardGroupSummaryResponse::getGroupName));

        return DashboardResponse.builder()
                .totalPaid(totalPaid)
                .totalOwes(totalOwes)
                .groupSummaries(groupSummaries)
                .personBalances(personBalances)
                .build();
    }
}
