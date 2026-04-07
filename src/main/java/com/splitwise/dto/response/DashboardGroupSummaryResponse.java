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
public class DashboardGroupSummaryResponse {

    private Long groupId;

    private String groupName;

    private BigDecimal totalExpense;

    private BigDecimal userNetBalance;
}
