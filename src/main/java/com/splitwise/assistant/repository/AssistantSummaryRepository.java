package com.splitwise.assistant.repository;

import com.splitwise.assistant.model.AssistantSummary;
import com.splitwise.assistant.model.AssistantSummaryPeriod;
import com.splitwise.models.User;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantSummaryRepository extends JpaRepository<AssistantSummary, Long> {

    Optional<AssistantSummary> findFirstByUserAndPeriodAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByCreatedAtDesc(
            User user,
            AssistantSummaryPeriod period,
            LocalDate start,
            LocalDate end
    );
}
