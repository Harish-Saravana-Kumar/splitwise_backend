package com.splitwise.repositories;

import com.splitwise.models.Settlement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByGroup_Id(Long groupId);

    List<Settlement> findByPayer_Id(Long payerId);

    List<Settlement> findByReceiver_Id(Long receiverId);

    List<Settlement> findByGroup_IdOrderBySettledAtDesc(Long groupId);

    void deleteByGroup_Id(Long groupId);
}