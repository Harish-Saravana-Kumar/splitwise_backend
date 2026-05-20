package com.splitwise.assistant.service;

import com.splitwise.assistant.config.AssistantProperties;
import com.splitwise.assistant.model.AssistantActionType;
import com.splitwise.assistant.model.PendingAssistantAction;
import com.splitwise.dto.request.ExpenseRequest;
import com.splitwise.dto.request.ExpenseSplitRequest;
import com.splitwise.dto.request.SettlementRequest;
import com.splitwise.models.Expense;
import com.splitwise.models.Group;
import com.splitwise.models.User;
import com.splitwise.repositories.ExpenseRepository;
import com.splitwise.repositories.GroupMemberRepository;
import com.splitwise.repositories.GroupRepository;
import com.splitwise.repositories.UserRepository;
import com.splitwise.services.ExpenseService;
import com.splitwise.services.SettlementService;
import com.splitwise.services.BalanceService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantPendingActionService {

    private final Map<String, PendingAssistantAction> pendingActions = new ConcurrentHashMap<>();

    private final AssistantProperties assistantProperties;
    private final ExpenseService expenseService;
    private final SettlementService settlementService;
    private final ExpenseRepository expenseRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final BalanceService balanceService;

    public PendingAssistantAction prepareCreateEqualExpense(
            String userEmail,
            Long groupId,
            Long paidByUserId,
            String description,
            BigDecimal amount,
            String splitType
    ) {
        String token = UUID.randomUUID().toString();

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group not found"));
        User payer = userRepository.findById(paidByUserId)
            .orElseThrow(() -> new RuntimeException("Payer user not found"));

        PendingAssistantAction action = PendingAssistantAction.builder()
                .token(token)
                .userEmail(userEmail)
                .actionType(AssistantActionType.CREATE_EQUAL_EXPENSE)
            .summary("Create " + splitType + " expense: group='" + group.getName() + "', payer='" + payer.getName() + "', amount=" + amount + ", description='" + description + "'")
                .expiresAt(LocalDateTime.now().plusSeconds(assistantProperties.getPendingActionTtlSeconds()))
                .payload(Map.of(
                        "groupId", groupId,
                        "paidByUserId", paidByUserId,
                        "description", description,
                        "amount", amount
                ))
                .build();

        pendingActions.put(token, action);
        return action;
    }

    public PendingAssistantAction prepareUpdateExpenseDescription(String userEmail, Long expenseId, String description) {
        String token = UUID.randomUUID().toString();

        PendingAssistantAction action = PendingAssistantAction.builder()
                .token(token)
                .userEmail(userEmail)
                .actionType(AssistantActionType.UPDATE_EXPENSE_DESCRIPTION)
                .summary("Update expense " + expenseId + " description to '" + description + "'")
                .expiresAt(LocalDateTime.now().plusSeconds(assistantProperties.getPendingActionTtlSeconds()))
                .payload(Map.of(
                        "expenseId", expenseId,
                        "description", description
                ))
                .build();

        pendingActions.put(token, action);
        return action;
    }

        public PendingAssistantAction prepareSettleUp(
            String userEmail,
            Long groupId,
            Long payerId,
            Long receiverId,
            BigDecimal amount
        ) {
        String token = UUID.randomUUID().toString();

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group not found"));
            User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User payer = userRepository.findById(payerId)
            .orElseThrow(() -> new RuntimeException("Payer user not found"));
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new RuntimeException("Receiver user not found"));

            boolean requesterCanSettle = requester.getId().equals(payerId);
            if (!requesterCanSettle) {
                throw new RuntimeException("Only the payer can settle up.");
            }

            if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, payerId)
                    || !groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, receiverId)) {
                throw new RuntimeException("Payer and receiver must be members of the selected group.");
            }

                BigDecimal pendingAmount = balanceService.getPendingSettlementBetween(groupId, payerId, receiverId);

            if (pendingAmount.compareTo(BigDecimal.ZERO) == 0) {
                throw new RuntimeException("Selected payer has no pending balance with the selected receiver in this group.");
            }

            if (amount.compareTo(pendingAmount) > 0) {
                throw new RuntimeException(
                        "Settlement amount cannot exceed pending balance between payer and receiver: " + pendingAmount
                );
            }

        PendingAssistantAction action = PendingAssistantAction.builder()
            .token(token)
            .userEmail(userEmail)
            .actionType(AssistantActionType.CREATE_SETTLEMENT)
            .summary("Settle up in group '" + group.getName() + "': payer='" + payer.getName() + "', receiver='"
                + receiver.getName() + "', amount=" + amount)
            .expiresAt(LocalDateTime.now().plusSeconds(assistantProperties.getPendingActionTtlSeconds()))
            .payload(Map.of(
                "groupId", groupId,
                "payerId", payerId,
                "receiverId", receiverId,
                "amount", amount
            ))
            .build();

        pendingActions.put(token, action);
        return action;
        }

    @Transactional
    public String execute(String userEmail, String token) {
        cleanupExpired();

        PendingAssistantAction action = pendingActions.get(token);
        if (action == null) {
            throw new RuntimeException("Confirmation token is invalid or expired");
        }

        if (!action.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("This confirmation token belongs to a different user");
        }

        if (action.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingActions.remove(token);
            throw new RuntimeException("Confirmation token expired. Please ask again.");
        }

        pendingActions.remove(token);

        return switch (action.getActionType()) {
            case CREATE_EQUAL_EXPENSE -> executeCreateEqualExpense(action, userEmail);
            case CREATE_SETTLEMENT -> executeSettleUp(action, userEmail);
            case UPDATE_EXPENSE_DESCRIPTION -> executeUpdateExpenseDescription(action, userEmail);
        };
    }

    private String executeCreateEqualExpense(PendingAssistantAction action, String userEmail) {
        Long groupId = ((Number) action.getPayload().get("groupId")).longValue();
        Long paidByUserId = ((Number) action.getPayload().get("paidByUserId")).longValue();
        String description = String.valueOf(action.getPayload().get("description"));
        BigDecimal amount = new BigDecimal(String.valueOf(action.getPayload().get("amount")));

        ExpenseRequest request = ExpenseRequest.builder()
                .groupId(groupId)
                .paidByUserId(paidByUserId)
                .description(description)
                .amount(amount)
                .splitType("EQUAL")
                .splits(List.<ExpenseSplitRequest>of())
                .build();

        var created = expenseService.addExpense(request, userEmail);
        String groupName = groupRepository.findById(created.getGroupId())
            .map(Group::getName)
            .orElse("selected group");
        return "Confirmed. Expense '" + created.getDescription() + "' for " + created.getAmount()
            + " was created in group '" + groupName + "'.";
    }

    private String executeUpdateExpenseDescription(PendingAssistantAction action, String userEmail) {
        Long expenseId = ((Number) action.getPayload().get("expenseId")).longValue();
        String newDescription = String.valueOf(action.getPayload().get("description"));

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isExpenseCreator = expense.getPaidBy().getId().equals(requester.getId());
        boolean isGroupCreator = expense.getGroup().getCreatedBy().getId().equals(requester.getId());
        if (!isExpenseCreator && !isGroupCreator) {
            throw new RuntimeException("Only expense creator or group creator can update description");
        }

        expense.setDescription(newDescription);
        expenseRepository.save(expense);

        return "Confirmed. Expense " + expenseId + " description updated.";
    }

        private String executeSettleUp(PendingAssistantAction action, String userEmail) {
        Long groupId = ((Number) action.getPayload().get("groupId")).longValue();
        Long payerId = ((Number) action.getPayload().get("payerId")).longValue();
        Long receiverId = ((Number) action.getPayload().get("receiverId")).longValue();
        BigDecimal amount = new BigDecimal(String.valueOf(action.getPayload().get("amount")));

        SettlementRequest request = SettlementRequest.builder()
            .groupId(groupId)
            .payerId(payerId)
            .receiverId(receiverId)
            .amount(amount)
            .build();

        var settled = settlementService.settleUp(request, userEmail);
        String groupName = groupRepository.findById(settled.getGroupId())
            .map(Group::getName)
            .orElse("selected group");
        BigDecimal pendingAfter = balanceService.getPendingSettlementBetween(groupId, payerId, receiverId);
        return "Confirmed. Payment of " + settled.getAmount() + " was applied in group '" + groupName + "' between "
            + settled.getPayer().getName() + " and " + settled.getReceiver().getName()
            + ". Remaining pending balance: " + pendingAfter + ".";
        }

    private void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        pendingActions.entrySet().removeIf(entry -> entry.getValue().getExpiresAt().isBefore(now));
    }
}
