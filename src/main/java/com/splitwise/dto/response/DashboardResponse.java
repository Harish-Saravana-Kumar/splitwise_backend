package com.splitwise.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private BigDecimal totalPaid;

    private BigDecimal totalOwes;

    private List<DashboardGroupSummaryResponse> groupSummaries;

    private List<DashboardPersonBalanceResponse> personBalances;
}
