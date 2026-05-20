package com.splitwise.assistant.repository;

import com.splitwise.assistant.model.AssistantConversationMemory;
import com.splitwise.models.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantConversationMemoryRepository extends JpaRepository<AssistantConversationMemory, Long> {

    List<AssistantConversationMemory> findTop10ByUserOrderByCreatedAtDesc(User user);
}
