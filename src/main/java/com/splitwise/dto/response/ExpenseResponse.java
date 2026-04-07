package com.splitwise.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {

    private Long id;

    private Long groupId;

    private UserResponse paidBy;

    private String description;

    private BigDecimal amount;

    private String splitType;

    private List<ExpenseSplitResponse> splits;

    private LocalDateTime createdAt;
}
