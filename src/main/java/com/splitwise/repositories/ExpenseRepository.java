package com.splitwise.repositories;

import com.splitwise.models.Expense;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroup_Id(Long groupId);

    List<Expense> findByPaidBy_Id(Long userId);

    List<Expense> findByGroup_IdOrderByCreatedAtDesc(Long groupId);

    @EntityGraph(attributePaths = {"group", "paidBy"})
    List<Expense> findByGroup_IdInOrderByCreatedAtDesc(List<Long> groupIds);

    @EntityGraph(attributePaths = {"group", "paidBy"})
    List<Expense> findByGroup_IdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<Long> groupIds,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("select e from Expense e join fetch e.group g join fetch e.paidBy p where g.id = :groupId order by e.createdAt desc")
    List<Expense> findByGroup_IdOrderByCreatedAtDescFetchGroupAndPaidBy(@Param("groupId") Long groupId);

    void deleteByGroup_Id(Long groupId);
}