package com.splitwise.assistant.repository;

import com.splitwise.assistant.model.AssistantChatMessage;
import com.splitwise.assistant.model.AssistantConversation;
import com.splitwise.assistant.model.AssistantMessageRole;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantChatMessageRepository extends JpaRepository<AssistantChatMessage, Long> {

    List<AssistantChatMessage> findByConversationOrderByCreatedAtDesc(AssistantConversation conversation, Pageable pageable);

    List<AssistantChatMessage> findByConversationUserEmailAndRoleOrderByCreatedAtDesc(
            String email,
            AssistantMessageRole role,
            Pageable pageable
    );
}
