package com.splitwise.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantChatRequest {

    private String conversationId;
    private String message;
    private Boolean confirm;
    private String confirmationToken;

    private String actionType;
    private CreateExpenseAction createExpense;
    private SettleUpAction settleUp;

    public boolean isConfirm() {
        return Boolean.TRUE.equals(confirm);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateExpenseAction {
        private Long groupId;
        private Long paidByUserId;
        private String description;
        private Double amount;
        private String splitType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SettleUpAction {
        private Long groupId;
        private Long payerId;
        private Long receiverId;
        private Double amount;
    }
}
