package com.splitwise.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRequest {

    @NotNull
    private Long groupId;

    @NotNull
    private Long payerId;

    @NotNull
    private Long receiverId; // optional: if null, treat as payer collecting across multiple receivers

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}
