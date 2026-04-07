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
public class DashboardPersonBalanceResponse {

    private Long userId;

    private String userName;

    private BigDecimal netAmount;
}
