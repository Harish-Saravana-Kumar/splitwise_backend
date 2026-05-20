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
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
        private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
        private final BalanceService balanceService;

    @Transactional
        public SettlementResponse settleUp(SettlementRequest request, String requesterEmail) {
        validateSettlementRequest(request);

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User payer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean canSettle = requester.getId().equals(payer.getId());
        if (!canSettle) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the payer can perform this settlement"
            );
        }

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), payer.getId())
                || !groupMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), receiver.getId())) {
            throw new RuntimeException("Payer and receiver must be members of the group");
        }

        BigDecimal pendingAmount = balanceService.getPendingSettlementBetween(
                request.getGroupId(),
                request.getPayerId(),
                request.getReceiverId()
        );

        if (pendingAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Payer has no pending balance with the selected receiver in this group");
        }
                if (request.getAmount().compareTo(pendingAmount) > 0) {
                        throw new RuntimeException("Settlement amount cannot exceed pending balance between payer and receiver: " + pendingAmount);
        }

                BigDecimal remainingAmount = request.getAmount();
                List<ExpenseSplit> payerUnsettledSplits = expenseSplitRepository.findByExpense_Group_Id(request.getGroupId()).stream()
                                .filter(split -> split.getUser().getId().equals(request.getPayerId()))
                                .filter(split -> split.getExpense().getPaidBy().getId().equals(request.getReceiverId()))
                                .filter(split -> !split.isSettled())
                                .sorted(Comparator
                                                .comparing((ExpenseSplit split) -> split.getExpense().getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder()))
                                                .thenComparing(ExpenseSplit::getId))
                                .toList();

                if (payerUnsettledSplits.isEmpty()) {
                        throw new RuntimeException("Payer has no pending balance with the selected receiver in this group");
                }

                for (ExpenseSplit split : payerUnsettledSplits) {
                        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                                break;
                        }

                        BigDecimal updatedAmount = split.getOwedAmount().subtract(remainingAmount);
                        if (updatedAmount.compareTo(BigDecimal.ZERO) > 0) {
                                split.setOwedAmount(updatedAmount);
                                remainingAmount = BigDecimal.ZERO;
                        } else {
                                split.setOwedAmount(BigDecimal.ZERO);
                                split.setSettled(true);
                                remainingAmount = updatedAmount.abs();
                        }
                }

                expenseSplitRepository.saveAll(payerUnsettledSplits);

                Settlement settlement = Settlement.builder()
                                .group(group)
                                .payer(payer)
                                .receiver(receiver)
                                .amount(request.getAmount())
                                .settledAt(LocalDateTime.now())
                                .build();
                Settlement savedSettlement = settlementRepository.save(settlement);

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