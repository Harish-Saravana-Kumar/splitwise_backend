package com.splitwise.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSplitResponse {

    private Long id;

    private UserResponse user;

    private BigDecimal owedAmount;

    private boolean settled;
}
