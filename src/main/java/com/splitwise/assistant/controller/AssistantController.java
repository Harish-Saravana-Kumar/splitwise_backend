package com.splitwise.assistant.controller;

import com.splitwise.assistant.dto.AssistantChatRequest;
import com.splitwise.assistant.dto.AssistantChatResponse;
import com.splitwise.assistant.service.AssistantAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantAgentService assistantAgentService;

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(
            @RequestBody AssistantChatRequest request,
            Authentication authentication
    ) {
        return new ResponseEntity<>(assistantAgentService.chat(authentication.getName(), request), HttpStatus.OK);
    }
}