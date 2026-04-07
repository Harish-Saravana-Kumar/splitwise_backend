package com.splitwise.repositories;

import com.splitwise.models.ExpenseSplit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    List<ExpenseSplit> findByExpense_Id(Long expenseId);

    void deleteByExpense_Id(Long expenseId);

    List<ExpenseSplit> findByUser_Id(Long userId);

    List<ExpenseSplit> findByUser_IdAndSettled(Long userId, boolean settled);

    List<ExpenseSplit> findByExpense_Group_Id(Long groupId);

    void deleteByExpense_Group_Id(Long groupId);
}