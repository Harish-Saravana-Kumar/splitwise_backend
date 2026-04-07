package com.splitwise.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementResponse {

    private Long id;

    private Long groupId;

    private UserResponse payer;

    private UserResponse receiver;

    private BigDecimal amount;

    private LocalDateTime settledAt;
}
