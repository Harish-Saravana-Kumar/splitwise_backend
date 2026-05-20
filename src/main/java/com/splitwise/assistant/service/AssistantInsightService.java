package com.splitwise.assistant.service;

import com.splitwise.assistant.dto.ExpenseSummary;
import com.splitwise.assistant.model.AssistantInsight;
import com.splitwise.assistant.model.AssistantInsightType;
import com.splitwise.assistant.repository.AssistantInsightRepository;
import com.splitwise.models.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantInsightService {

    private final AssistantQueryService assistantQueryService;
    private final AssistantInsightRepository assistantInsightRepository;

    @Transactional
    public List<String> getMonthlyInsights(User user) {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDateTime start = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime end = lastMonth.atEndOfMonth().atTime(23, 59, 59);

        List<ExpenseSummary> expenses = assistantQueryService.getExpenseSummariesForUserInRange(user.getId(), start, end);
        if (expenses.isEmpty()) {
            return List.of("No expenses found for last month, so I could not generate insights yet.");
        }

        List<String> insights = new ArrayList<>();

        BigDecimal total = expenses.stream()
                .map(ExpenseSummary::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byGroup = expenses.stream()
                .collect(Collectors.groupingBy(ExpenseSummary::groupName,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseSummary::amount, BigDecimal::add)));

        Map.Entry<String, BigDecimal> topGroup = byGroup.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (topGroup != null) {
            insights.add("Top group spend last month: " + topGroup.getKey() + " (" + topGroup.getValue() + ").");
            saveInsight(user, AssistantInsightType.TOP_GROUP, "Top group spend: " + topGroup.getKey());
        }

        ExpenseSummary largest = expenses.stream()
                .max((a, b) -> a.amount().compareTo(b.amount()))
                .orElse(null);
        if (largest != null) {
            insights.add("Largest expense: " + largest.title() + " (" + largest.amount() + ") in " + largest.groupName() + ".");
            saveInsight(user, AssistantInsightType.LARGE_EXPENSE, "Largest expense: " + largest.title());
        }

        YearMonth previous = lastMonth.minusMonths(1);
        LocalDateTime prevStart = previous.atDay(1).atStartOfDay();
        LocalDateTime prevEnd = previous.atEndOfMonth().atTime(23, 59, 59);
        BigDecimal prevTotal = assistantQueryService.getTotalSpentForUserInRange(user.getId(), prevStart, prevEnd);

        if (prevTotal.compareTo(BigDecimal.ZERO) > 0 && total.compareTo(prevTotal.multiply(BigDecimal.valueOf(1.25))) > 0) {
            insights.add("You spent about 25%+ more than the previous month (" + prevTotal + " -> " + total + ").");
            saveInsight(user, AssistantInsightType.SPENDING_SPIKE, "Spending spike vs previous month.");
        }

        return insights;
    }

    private void saveInsight(User user, AssistantInsightType type, String message) {
        AssistantInsight insight = AssistantInsight.builder()
                .user(user)
                .insightType(type)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        assistantInsightRepository.save(insight);
    }
}
