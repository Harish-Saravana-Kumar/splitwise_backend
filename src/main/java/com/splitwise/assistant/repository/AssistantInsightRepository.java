package com.splitwise.assistant.repository;

import com.splitwise.assistant.model.AssistantInsight;
import com.splitwise.models.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantInsightRepository extends JpaRepository<AssistantInsight, Long> {

    List<AssistantInsight> findTop5ByUserOrderByCreatedAtDesc(User user);
}
