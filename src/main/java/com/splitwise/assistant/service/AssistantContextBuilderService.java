package com.splitwise.assistant.service;

import com.splitwise.assistant.config.AssistantProperties;
import com.splitwise.assistant.dto.AssistantFinancialContext;
import com.splitwise.assistant.dto.ExpenseSummary;
import com.splitwise.models.User;
import com.splitwise.repositories.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantContextBuilderService {

    private final AssistantQueryService assistantQueryService;
    private final AssistantInsightService assistantInsightService;
    private final AssistantRecommendationService assistantRecommendationService;
    private final AssistantSummaryService assistantSummaryService;
    private final UserRepository userRepository;
    private final AssistantProperties assistantProperties;

    @Transactional
    public AssistantFinancialContext buildContextForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int limit = Math.max(1, assistantProperties.getRecentExpenseLimit());
        List<ExpenseSummary> recentExpenses = assistantQueryService.getRecentExpenseSummariesForUser(user.getId(), limit);

        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDateTime start = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime end = lastMonth.atEndOfMonth().atTime(23, 59, 59);
        BigDecimal totalSpent = assistantQueryService.getTotalSpentForUserInRange(user.getId(), start, end);

        String topCategory = computeTopCategory(recentExpenses);
        List<String> insights = assistantInsightService.getMonthlyInsights(user);
        List<String> recommendations = assistantRecommendationService.getRecommendations(totalSpent, topCategory, insights);
        String monthlySummary = assistantSummaryService.getOrCreateMonthlySummary(user);

        return new AssistantFinancialContext(
                totalSpent,
                topCategory,
                recentExpenses,
                insights,
                recommendations,
                monthlySummary
        );
    }

    private String computeTopCategory(List<ExpenseSummary> recentExpenses) {
        if (recentExpenses == null || recentExpenses.isEmpty()) {
            return "Unknown";
        }

        Map<String, BigDecimal> byGroup = recentExpenses.stream()
                .collect(Collectors.groupingBy(ExpenseSummary::groupName,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseSummary::amount, BigDecimal::add)));

        return byGroup.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
    }
}
