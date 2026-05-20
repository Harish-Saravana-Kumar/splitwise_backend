package com.splitwise.assistant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpenseSummary(
        String title,
        BigDecimal amount,
        String paidBy,
        String groupName,
        LocalDateTime createdAt
) {
}
