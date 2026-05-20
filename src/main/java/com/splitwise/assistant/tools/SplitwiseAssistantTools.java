package com.splitwise.assistant.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitwise.assistant.context.AssistantUserContext;
import com.splitwise.assistant.dto.AssistantFinancialContext;
import com.splitwise.assistant.dto.ExpenseSummary;
import com.splitwise.assistant.model.PendingAssistantAction;
import com.splitwise.assistant.service.AssistantContextBuilderService;
import com.splitwise.assistant.service.AssistantPendingActionService;
import com.splitwise.dto.response.DashboardGroupSummaryResponse;
import com.splitwise.dto.response.DashboardPersonBalanceResponse;
import com.splitwise.dto.response.DashboardResponse;
import com.splitwise.services.DashboardService;
import dev.langchain4j.agent.tool.Tool;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SplitwiseAssistantTools {

    private static final DateTimeFormatter RECENT_EXPENSES_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final DashboardService dashboardService;
    private final AssistantContextBuilderService assistantContextBuilderService;
    private final AssistantPendingActionService pendingActionService;
    private final ObjectMapper objectMapper;

    @Tool("Get the current user's total paid, total owes, net, and brief group-level summary.")
    public String getMyFinancialSummary() {
        String userEmail = requiredUserEmail();
        DashboardResponse dashboard = dashboardService.getDashboardByUserEmail(userEmail);

        StringBuilder out = new StringBuilder();
        out.append("Total paid: ").append(dashboard.getTotalPaid()).append(". ");
        out.append("Total owes: ").append(dashboard.getTotalOwes()).append(". ");
        out.append("Net: ").append(dashboard.getTotalPaid().subtract(dashboard.getTotalOwes())).append(". ");

        if (!dashboard.getGroupSummaries().isEmpty()) {
            out.append("Groups: ");
            for (DashboardGroupSummaryResponse group : dashboard.getGroupSummaries()) {
                out.append("[")
                        .append(group.getGroupName())
                        .append(" net=")
                        .append(group.getUserNetBalance())
                        .append("] ");
            }
        }

        return out.toString().trim();
    }

    @Tool("Get who owes or is owed by the current user across all groups.")
    public String getWhoOwesWhom() {
        String userEmail = requiredUserEmail();
        DashboardResponse dashboard = dashboardService.getDashboardByUserEmail(userEmail);

        List<DashboardPersonBalanceResponse> rows = dashboard.getPersonBalances();
        if (rows.isEmpty()) {
            return "No pending person-to-person balances.";
        }

        StringBuilder out = new StringBuilder("Current dues:\n");
        for (DashboardPersonBalanceResponse row : rows) {
            BigDecimal net = row.getNetAmount();
            if (net.signum() < 0) {
                out.append("- You owe ").append(row.getUserName()).append(" ").append(net.abs()).append(".\n");
            } else {
                out.append("- ").append(row.getUserName()).append(" owes you ").append(net).append(".\n");
            }
        }
        return out.toString().trim();
    }

    @Tool("Get recent expenses for the current user. Use limit between 1 and 20.")
    public String getRecentExpenses(int limit) {
        String userEmail = requiredUserEmail();
        int safeLimit = Math.max(1, Math.min(limit, 20));

        AssistantFinancialContext context = assistantContextBuilderService.buildContextForUser(userEmail);
        List<ExpenseSummary> expenses = context.recentExpenses();
        if (expenses == null || expenses.isEmpty()) {
            return "No recent expenses found.";
        }

        StringBuilder out = new StringBuilder("Your most recent expenses:\n");
        for (ExpenseSummary expense : expenses.stream().limit(safeLimit).toList()) {
            out.append("- ")
                .append(expense.title())
                    .append(" | Paid by ")
                .append(expense.paidBy())
                    .append(" | ")
                .append(expense.amount())
                    .append(" | Group: ")
                .append(expense.groupName())
                    .append(" | ")
                .append(expense.createdAt() != null ? expense.createdAt().format(RECENT_EXPENSES_DATE_FORMAT) : "Date unknown")
                    .append("\n");
        }

        return out.toString().trim();
    }

    @Tool("Get concise spending insights for the current user: totals, average, top groups, and notable expenses.")
    public String getSpendingInsights() {
        String userEmail = requiredUserEmail();

        AssistantFinancialContext context = assistantContextBuilderService.buildContextForUser(userEmail);
        StringBuilder sb = new StringBuilder();
        sb.append("Spending insights:\n");

        if (context.insights() == null || context.insights().isEmpty()) {
            sb.append("- No insights available yet.\n");
        } else {
            for (String insight : context.insights()) {
                sb.append("- ").append(insight).append("\n");
            }
        }

        if (context.recommendations() != null && !context.recommendations().isEmpty()) {
            sb.append("Recommendations:\n");
            for (String recommendation : context.recommendations()) {
                sb.append("- ").append(recommendation).append("\n");
            }
        }

        return sb.toString().trim();
    }

    @Tool("Get structured financial context for the current user. Returns JSON with totals, recent expenses, insights, recommendations, and summary.")
    public String getFinancialContext() {
        String userEmail = requiredUserEmail();
        AssistantFinancialContext context = assistantContextBuilderService.buildContextForUser(userEmail);
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException ex) {
            return "Unable to serialize context. Summary: totalSpent=" + context.totalSpent() + ", topCategory=" + context.topCategory();
        }
    }

    @Tool("Prepare creating an EQUAL split expense. This does not write immediately; it returns a confirmation token.")
    public String prepareCreateEqualExpense(Long groupId, Long paidByUserId, String description, double amount) {
        String userEmail = requiredUserEmail();
        if (amount <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        PendingAssistantAction action = pendingActionService.prepareCreateEqualExpense(
                userEmail,
                groupId,
                paidByUserId,
                description,
            BigDecimal.valueOf(amount),
            "EQUAL"
        );

        return "Pending action created. Ask user to confirm with token: " + action.getToken() + ". Action: " + action.getSummary();
    }

    @Tool("Prepare updating an expense description by expense ID. This does not write immediately; it returns a confirmation token.")
    public String prepareUpdateExpenseDescription(Long expenseId, String description) {
        String userEmail = requiredUserEmail();
        PendingAssistantAction action = pendingActionService.prepareUpdateExpenseDescription(userEmail, expenseId, description);
        return "Pending action created. Ask user to confirm with token: " + action.getToken() + ". Action: " + action.getSummary();
    }

    private String requiredUserEmail() {
        String userEmail = AssistantUserContext.getUserEmail();
        if (userEmail == null || userEmail.isBlank()) {
            throw new RuntimeException("Missing assistant user context");
        }
        return userEmail;
    }

}
