package com.splitwise.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {

    private boolean enabled = true;
    private String provider = "groq";
    private String model = "openai/gpt-oss-120b";
    private String apiKey;
    private String baseUrl = "https://api.groq.com/openai/v1";
    private double temperature = 0.2;
    private int maxOutputTokens = 1024;
    private int timeoutSeconds = 20;
    private int pendingActionTtlSeconds = 300;
    private int historyLoadLimit = 20;
    private int memoryWindowMessages = 12;
    private int recentExpenseLimit = 10;
    private Google google = new Google();

    @Data
    public static class Google {
        private String apiKey;
    }
}
