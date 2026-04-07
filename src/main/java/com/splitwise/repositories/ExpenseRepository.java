package com.splitwise.repositories;

import com.splitwise.models.Expense;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroup_Id(Long groupId);

    List<Expense> findByPaidBy_Id(Long userId);

    List<Expense> findByGroup_IdOrderByCreatedAtDesc(Long groupId);

    void deleteByGroup_Id(Long groupId);
}