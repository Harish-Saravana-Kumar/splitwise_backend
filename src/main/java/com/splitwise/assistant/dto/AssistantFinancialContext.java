package com.splitwise.assistant.dto;

import java.math.BigDecimal;
import java.util.List;

public record AssistantFinancialContext(
        BigDecimal totalSpent,
        String topCategory,
        List<ExpenseSummary> recentExpenses,
        List<String> insights,
        List<String> recommendations,
        String monthlySummary
) {
}
