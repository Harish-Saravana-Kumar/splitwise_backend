package com.splitwise.services;

import com.splitwise.dto.request.SettlementRequest;
import com.splitwise.dto.response.SettlementResponse;
import com.splitwise.dto.response.UserResponse;
import com.splitwise.models.ExpenseSplit;
import com.splitwise.models.Group;
import com.splitwise.models.Settlement;
import com.splitwise.models.User;
import com.splitwise.repositories.ExpenseSplitRepository;
import com.splitwise.repositories.GroupMemberRepository;
import com.splitwise.repositories.GroupRepository;
import com.splitwise.repositories.SettlementRepository;
import com.splitwise.repositories.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
        private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseSplitRepository expenseSplitRepository;

    @Transactional
    public SettlementResponse settleUp(SettlementRequest request) {
        validateSettlementRequest(request);

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User payer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), payer.getId())
                || !groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), receiver.getId())) {
            throw new RuntimeException("Payer and receiver must be members of the group");
        }

        Settlement settlement = Settlement.builder()
                .group(group)
                .payer(payer)
                .receiver(receiver)
                .amount(request.getAmount())
                .settledAt(LocalDateTime.now())
                .build();
        Settlement savedSettlement = settlementRepository.save(settlement);

        List<ExpenseSplit> unsettledSplitsForGroup = expenseSplitRepository.findByExpense_Group_Id(request.getGroupId());
        List<ExpenseSplit> payerUnsettledSplits = unsettledSplitsForGroup.stream()
                .filter(split -> split.getUser().getId().equals(request.getPayerId()))
                .filter(split -> !split.isSettled())
                .toList();

        BigDecimal pendingAmount = payerUnsettledSplits.stream()
                .map(ExpenseSplit::getOwedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (pendingAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Payer has no pending balance in this group");
        }
        if (request.getAmount().compareTo(pendingAmount) != 0) {
            throw new RuntimeException("Settlement amount must exactly match payer pending balance");
        }

        payerUnsettledSplits.forEach(split -> split.setSettled(true));
        expenseSplitRepository.saveAll(payerUnsettledSplits);

        return mapToSettlementResponse(savedSettlement);
    }

    public List<SettlementResponse> getSettlementsByGroup(Long groupId) {
        return settlementRepository.findByGroup_IdOrderBySettledAtDesc(groupId)
                .stream()
                .map(this::mapToSettlementResponse)
                .toList();
    }

        private void validateSettlementRequest(SettlementRequest request) {
                if (request.getPayerId().equals(request.getReceiverId())) {
                        throw new RuntimeException("Payer and receiver cannot be the same user");
                }
                if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new RuntimeException("Settlement amount must be greater than zero");
                }
        }

    private SettlementResponse mapToSettlementResponse(Settlement settlement) {
        return SettlementResponse.builder()
                .id(settlement.getId())
                .groupId(settlement.getGroup().getId())
                .payer(mapToUserResponse(settlement.getPayer()))
                .receiver(mapToUserResponse(settlement.getReceiver()))
                .amount(settlement.getAmount())
                .settledAt(settlement.getSettledAt())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}