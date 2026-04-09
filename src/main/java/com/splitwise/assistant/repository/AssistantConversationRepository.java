package com.splitwise.assistant.repository;

import com.splitwise.assistant.model.AssistantConversation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, String> {

    Optional<AssistantConversation> findByIdAndUserEmail(String id, String email);
}
