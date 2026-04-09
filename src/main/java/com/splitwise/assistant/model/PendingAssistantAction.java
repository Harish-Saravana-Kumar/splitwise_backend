package com.splitwise.assistant.model;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PendingAssistantAction {

    private String token;
    private String userEmail;
    private AssistantActionType actionType;
    private String summary;
    private LocalDateTime expiresAt;
    private Map<String, Object> payload;
}
