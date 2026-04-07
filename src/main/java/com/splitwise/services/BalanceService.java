package com.splitwise.services;

import com.splitwise.dto.response.SettlementResponse;
import com.splitwise.dto.response.UserResponse;
import com.splitwise.models.ExpenseSplit;
import com.splitwise.models.User;
import com.splitwise.repositories.ExpenseSplitRepository;
import com.splitwise.repositories.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseSplitRepository expenseSplitRepository;
    private final UserRepository userRepository;

    public Map<Long, BigDecimal> getGroupBalances(Long groupId) {
        List<ExpenseSplit> splits = expenseSplitRepository.findByExpense_Group_Id(groupId);
        Map<Long, BigDecimal> balances = new HashMap<>();

        for (ExpenseSplit split : splits) {
            if (split.isSettled()) {
                continue;
            }

            Long owingUserId = split.getUser().getId();
            Long paidByUserId = split.getExpense().getPaidBy().getId();
            BigDecimal amount = split.getOwedAmount();

            balances.merge(owingUserId, amount.negate(), BigDecimal::add);
            balances.merge(paidByUserId, amount, BigDecimal::add);
        }

        return balances;
    }

    public List<SettlementResponse> getMinimumSettlements(Long groupId) {
        Map<Long, BigDecimal> balances = getGroupBalances(groupId);
        List<BalanceEntry> creditors = new ArrayList<>();
        List<BalanceEntry> debtors = new ArrayList<>();

        balances.forEach((userId, balance) -> {
            int comparison = balance.compareTo(BigDecimal.ZERO);
            if (comparison > 0) {
                creditors.add(new BalanceEntry(userId, balance));
            } else if (comparison < 0) {
                debtors.add(new BalanceEntry(userId, balance.abs()));
            }
        });

        Map<Long, UserResponse> userCache = new HashMap<>();
        List<SettlementResponse> suggestions = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            creditors.sort(Comparator.comparing(BalanceEntry::balance).reversed());
            debtors.sort(Comparator.comparing(BalanceEntry::balance).reversed());

            BalanceEntry creditor = creditors.getFirst();
            BalanceEntry debtor = debtors.getFirst();
            BigDecimal settleAmount = creditor.balance().min(debtor.balance());

            suggestions.add(SettlementResponse.builder()
                    .groupId(groupId)
                    .payer(getUserResponseCached(debtor.userId(), userCache))
                    .receiver(getUserResponseCached(creditor.userId(), userCache))
                    .amount(settleAmount)
                    .settledAt(LocalDateTime.now())
                    .build());

            BigDecimal creditorRemaining = creditor.balance().subtract(settleAmount);
            BigDecimal debtorRemaining = debtor.balance().subtract(settleAmount);

            creditors.removeFirst();
            debtors.removeFirst();

            if (creditorRemaining.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new BalanceEntry(creditor.userId(), creditorRemaining));
            }
            if (debtorRemaining.compareTo(BigDecimal.ZERO) > 0) {
                debtors.add(new BalanceEntry(debtor.userId(), debtorRemaining));
            }
        }

        return suggestions;
    }

    private UserResponse getUserResponseCached(Long userId, Map<Long, UserResponse> cache) {
        return cache.computeIfAbsent(userId, id -> {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return UserResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .build();
        });
    }

    @AllArgsConstructor
    private static class BalanceEntry {
        private final Long userId;
        private final BigDecimal balance;

        public Long userId() {
            return userId;
        }

        public BigDecimal balance() {
            return balance;
        }
    }
}