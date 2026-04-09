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
public class SettlementBalanceResponse {

    private Long groupId;

    private Long payerId;

    private Long receiverId;

    private BigDecimal amount;
}