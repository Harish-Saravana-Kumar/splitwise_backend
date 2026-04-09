package com.splitwise.assistant.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantChatResponse {

    private String conversationId;
    private String message;
    private boolean requiresConfirmation;
    private String confirmationToken;
    private String actionType;
    private Map<String, Object> actionData;
}
