package com.splitwise.assistant.service;

import com.splitwise.assistant.config.AssistantProperties;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssistantMemoryService {

    private final AssistantProperties assistantProperties;
    private final Map<String, ChatMemory> memoryByUser = new ConcurrentHashMap<>();

    public ChatMemory getMemoryForUser(String userEmail) {
        return memoryByUser.computeIfAbsent(userEmail, ignored ->
                MessageWindowChatMemory.withMaxMessages(assistantProperties.getMemoryWindowMessages())
        );
    }
}
