package com.splitwise.assistant.service;

import com.splitwise.assistant.dto.ExpenseSummary;
import com.splitwise.assistant.model.AssistantSummary;
import com.splitwise.assistant.model.AssistantSummaryPeriod;
import com.splitwise.assistant.repository.AssistantSummaryRepository;
import com.splitwise.models.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantSummaryService {

    private final AssistantQueryService assistantQueryService;
    private final AssistantSummaryRepository assistantSummaryRepository;

    @Transactional
    public String getOrCreateMonthlySummary(User user) {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate periodStart = lastMonth.atDay(1);
        LocalDate periodEnd = lastMonth.atEndOfMonth();

        Optional<AssistantSummary> existing = assistantSummaryRepository
                .findFirstByUserAndPeriodAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByCreatedAtDesc(
                        user,
                        AssistantSummaryPeriod.MONTHLY,
                        periodStart,
                        periodEnd
                );

        if (existing.isPresent()) {
            return existing.get().getSummaryText();
        }

        LocalDateTime start = periodStart.atStartOfDay();
        LocalDateTime end = periodEnd.atTime(23, 59, 59);
        List<ExpenseSummary> expenses = assistantQueryService.getExpenseSummariesForUserInRange(user.getId(), start, end);

        if (expenses.isEmpty()) {
            return "No expenses found last month to generate a summary.";
        }

        BigDecimal total = expenses.stream()
                .map(ExpenseSummary::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byGroup = expenses.stream()
                .collect(Collectors.groupingBy(ExpenseSummary::groupName,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseSummary::amount, BigDecimal::add)));

        Map.Entry<String, BigDecimal> topGroup = byGroup.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        ExpenseSummary largest = expenses.stream()
                .max(Comparator.comparing(ExpenseSummary::amount))
                .orElse(null);

        String summary = "Monthly Summary:\n"
                + "- Total spent: " + total + "\n"
                + (topGroup != null ? "- Highest group: " + topGroup.getKey() + " (" + topGroup.getValue() + ")\n" : "")
                + (largest != null ? "- Largest expense: " + largest.title() + " (" + largest.amount() + ")\n" : "");

        AssistantSummary saved = AssistantSummary.builder()
                .user(user)
                .period(AssistantSummaryPeriod.MONTHLY)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .totalSpent(total)
                .topCategory(topGroup != null ? topGroup.getKey() : null)
                .largestExpenseDescription(largest != null ? largest.title() : null)
                .summaryText(summary)
                .createdAt(LocalDateTime.now())
                .build();

        assistantSummaryRepository.save(saved);
        return summary;
    }
}
