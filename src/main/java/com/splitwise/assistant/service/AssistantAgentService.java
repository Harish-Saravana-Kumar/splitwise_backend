package com.splitwise.assistant.service;

import com.splitwise.assistant.config.AssistantProperties;
import com.splitwise.assistant.context.AssistantUserContext;
import com.splitwise.assistant.dto.AssistantChatRequest;
import com.splitwise.assistant.dto.AssistantChatResponse;
import com.splitwise.assistant.model.AssistantChatMessage;
import com.splitwise.assistant.model.AssistantConversation;
import com.splitwise.assistant.model.AssistantMessageRole;
import com.splitwise.assistant.model.PendingAssistantAction;
import com.splitwise.assistant.repository.AssistantChatMessageRepository;
import com.splitwise.assistant.repository.AssistantConversationRepository;
import com.splitwise.assistant.tools.SplitwiseAssistantTools;
import com.splitwise.models.User;
import com.splitwise.repositories.UserRepository;
import com.splitwise.assistant.service.AssistantMemoryService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssistantAgentService {

    public static final String ACTION_CREATE_EQUAL_EXPENSE_FORM = "CREATE_EQUAL_EXPENSE_FORM";
    public static final String ACTION_CREATE_EQUAL_EXPENSE_SUBMIT = "CREATE_EQUAL_EXPENSE_SUBMIT";
    public static final String ACTION_SETTLE_UP_FORM = "SETTLE_UP_FORM";
    public static final String ACTION_SETTLE_UP_SUBMIT = "SETTLE_UP_SUBMIT";
    private static final int MAX_HISTORY_MESSAGE_CHARS = 320;
    private static final int MAX_TOTAL_PROMPT_CHARS = 2200;
    private static final int LAST_QUESTION_LOOKBACK = 30;
    private static final Pattern RETRY_SECONDS_PATTERN = Pattern.compile("try again in ([0-9]+(?:\\.[0-9]+)?)s", Pattern.CASE_INSENSITIVE);

    private final AssistantProperties assistantProperties;
    private final AssistantPendingActionService pendingActionService;
    private final AssistantMemoryService assistantMemoryService;
    private final SplitwiseAssistantTools splitwiseAssistantTools;
    private final AssistantConversationRepository assistantConversationRepository;
    private final AssistantChatMessageRepository assistantChatMessageRepository;
    private final UserRepository userRepository;
    private final Environment environment;

    private SplitwiseChatAgent splitwiseChatAgent;
    private SplitwisePlainAgent splitwisePlainAgent;

    private SplitwiseChatAgent getAgent() {
        OpenAiChatModel chatLanguageModel = buildModelIfConfigured();
        if (splitwiseChatAgent == null && chatLanguageModel != null) {
            splitwiseChatAgent = AiServices.builder(SplitwiseChatAgent.class)
                    .chatModel(chatLanguageModel)
                    .tools(splitwiseAssistantTools)
                    .chatMemoryProvider(memoryId -> assistantMemoryService.getMemoryForUser(String.valueOf(memoryId)))
                    .build();
        }
        return splitwiseChatAgent;
    }

    private SplitwisePlainAgent getPlainAgent() {
        OpenAiChatModel chatLanguageModel = buildModelIfConfigured();
        if (splitwisePlainAgent == null && chatLanguageModel != null) {
            splitwisePlainAgent = AiServices.builder(SplitwisePlainAgent.class)
                    .chatModel(chatLanguageModel)
                    .chatMemoryProvider(memoryId -> assistantMemoryService.getMemoryForUser(String.valueOf(memoryId)))
                    .build();
        }
        return splitwisePlainAgent;
    }

    private OpenAiChatModel buildModelIfConfigured() {
        if (!assistantProperties.isEnabled()) {
            return null;
        }

        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(resolveBaseUrl())
                .modelName(resolveModelName())
                .temperature(resolveTemperature())
                .maxTokens(resolveMaxOutputTokens())
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                .build();
    }

    private String resolveApiKey() {
        if (StringUtils.hasText(assistantProperties.getApiKey())) {
            return assistantProperties.getApiKey();
        }

        if (StringUtils.hasText(assistantProperties.getGoogle().getApiKey())) {
            return assistantProperties.getGoogle().getApiKey();
        }

        String fromOpenAi = environment.getProperty("langchain4j.open-ai.chat-model.api-key");
        if (StringUtils.hasText(fromOpenAi)) {
            return fromOpenAi;
        }

        String fromLangChain = environment.getProperty("langchain4j.google-ai.chat-model.api-key");
        if (StringUtils.hasText(fromLangChain)) {
            return fromLangChain;
        }

        String fromGroqEnv = System.getenv("GROQ_API_KEY");
        if (StringUtils.hasText(fromGroqEnv)) {
            return fromGroqEnv;
        }

        String fromOpenAiEnv = System.getenv("OPENAI_API_KEY");
        if (StringUtils.hasText(fromOpenAiEnv)) {
            return fromOpenAiEnv;
        }

        return System.getenv("GOOGLE_API_KEY");
    }

    private String resolveBaseUrl() {
        if (StringUtils.hasText(assistantProperties.getBaseUrl())) {
            return assistantProperties.getBaseUrl();
        }

        String fromOpenAi = environment.getProperty("langchain4j.open-ai.chat-model.base-url");
        if (StringUtils.hasText(fromOpenAi)) {
            return fromOpenAi;
        }

        return "https://api.groq.com/openai/v1";
    }

    private String resolveModelName() {
        String fromAssistant = assistantProperties.getModel();
        if (StringUtils.hasText(fromAssistant)) {
            return fromAssistant;
        }

        String fromOpenAi = environment.getProperty("langchain4j.open-ai.chat-model.model-name");
        if (StringUtils.hasText(fromOpenAi)) {
            return fromOpenAi;
        }

        String fromLangChain = environment.getProperty("langchain4j.google-ai.chat-model.model-name");
        if (StringUtils.hasText(fromLangChain)) {
            return fromLangChain;
        }

        return "openai/gpt-oss-120b";
    }

    private double resolveTemperature() {
        String fromOpenAi = environment.getProperty("langchain4j.open-ai.chat-model.temperature");
        if (StringUtils.hasText(fromOpenAi)) {
            try {
                return Double.parseDouble(fromOpenAi);
            } catch (NumberFormatException ignored) {
                // Fall back to assistant properties value.
            }
        }

        String fromLangChain = environment.getProperty("langchain4j.google-ai.chat-model.temperature");
        if (StringUtils.hasText(fromLangChain)) {
            try {
                return Double.parseDouble(fromLangChain);
            } catch (NumberFormatException ignored) {
                // Fall back to assistant properties value.
            }
        }
        return assistantProperties.getTemperature();
    }

    private int resolveMaxOutputTokens() {
        String fromOpenAiCompletion = environment.getProperty("langchain4j.open-ai.chat-model.max-completion-tokens");
        if (StringUtils.hasText(fromOpenAiCompletion)) {
            try {
                return Integer.parseInt(fromOpenAiCompletion);
            } catch (NumberFormatException ignored) {
                // Fall back to other values.
            }
        }

        String fromOpenAi = environment.getProperty("langchain4j.open-ai.chat-model.max-tokens");
        if (StringUtils.hasText(fromOpenAi)) {
            try {
                return Integer.parseInt(fromOpenAi);
            } catch (NumberFormatException ignored) {
                // Fall back to assistant properties value.
            }
        }

        String fromLangChain = environment.getProperty("langchain4j.google-ai.chat-model.max-output-tokens");
        if (StringUtils.hasText(fromLangChain)) {
            try {
                return Integer.parseInt(fromLangChain);
            } catch (NumberFormatException ignored) {
                // Fall back to assistant properties value.
            }
        }
        return assistantProperties.getMaxOutputTokens();
    }

    private int resolveTimeoutSeconds() {
        String fromOpenAi = environment.getProperty("langchain4j.open-ai.chat-model.timeout");
        if (StringUtils.hasText(fromOpenAi)) {
            String normalized = fromOpenAi.trim().toLowerCase();
            if (normalized.endsWith("s")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            try {
                return Integer.parseInt(normalized);
            } catch (NumberFormatException ignored) {
                // Fall back to assistant properties value.
            }
        }

        String fromLangChain = environment.getProperty("langchain4j.google-ai.chat-model.timeout");
        if (StringUtils.hasText(fromLangChain)) {
            String normalized = fromLangChain.trim().toLowerCase();
            if (normalized.endsWith("s")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            try {
                return Integer.parseInt(normalized);
            } catch (NumberFormatException ignored) {
                // Fall back to assistant properties value.
            }
        }
        return assistantProperties.getTimeoutSeconds();
    }

    @Transactional
    public AssistantChatResponse chat(String userEmail, AssistantChatRequest request) {
        String conversationId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        AssistantConversation conversation = getOrCreateConversation(userEmail, conversationId);

        if (ACTION_CREATE_EQUAL_EXPENSE_SUBMIT.equalsIgnoreCase(request.getActionType())) {
            return handleCreateExpenseSubmit(conversation, conversationId, userEmail, request);
        }
        if (ACTION_SETTLE_UP_SUBMIT.equalsIgnoreCase(request.getActionType())) {
            return handleSettleUpSubmit(conversation, conversationId, userEmail, request);
        }

        if (request.isConfirm()) {
            if (!StringUtils.hasText(request.getConfirmationToken())) {
                throw new RuntimeException("confirmationToken is required when confirm=true");
            }

            saveMessage(conversation, AssistantMessageRole.USER, "CONFIRM_ACTION token=" + request.getConfirmationToken());

            String result = pendingActionService.execute(userEmail, request.getConfirmationToken());
            saveMessage(conversation, AssistantMessageRole.ASSISTANT, result);
            return AssistantChatResponse.builder()
                    .conversationId(conversationId)
                    .message(result)
                    .requiresConfirmation(false)
                    .confirmationToken(null)
                    .actionType(null)
                    .actionData(null)
                    .build();
        }

        if (!StringUtils.hasText(request.getMessage())) {
            throw new RuntimeException("message is required");
        }

        if (isLastQuestionIntent(request.getMessage())) {
            AssistantChatResponse lastQuestionResponse = handleLastQuestionRequest(conversation, conversationId, userEmail, request.getMessage());
            if (lastQuestionResponse != null) {
                return lastQuestionResponse;
            }
        }

        if (isSessionInfoIntent(request.getMessage())) {
            return handleSessionInfoRequest(conversation, conversationId, userEmail);
        }

        if (isAssistantHelpIntent(request.getMessage())) {
            return handleAssistantHelpRequest(conversation, conversationId, request.getMessage());
        }

        if (isFinancialAnalysisIntent(request.getMessage())) {
            return handleFinancialAnalysisRequest(conversation, conversationId, userEmail, request.getMessage());
        }

        if (isDirectBalanceIntent(request.getMessage())) {
            return handleDirectBalanceRequest(conversation, conversationId, request.getMessage(), userEmail);
        }

        if (isCreateExpenseIntent(request.getMessage())) {
            String helperMessage = "Sure. Please fill the expense form below. "
                + "You only need to provide group name, payer, split type, description, and amount.";
            saveMessage(conversation, AssistantMessageRole.USER, request.getMessage());
            saveMessage(conversation, AssistantMessageRole.ASSISTANT, helperMessage);
            return AssistantChatResponse.builder()
                    .conversationId(conversationId)
                    .message(helperMessage)
                    .requiresConfirmation(false)
                    .confirmationToken(null)
                    .actionType(ACTION_CREATE_EQUAL_EXPENSE_FORM)
                .actionData(Map.of("defaultSplitType", "EQUAL", "supportedSplitTypes", java.util.List.of("EQUAL")))
                    .build();
        }

                if (isSettleUpIntent(request.getMessage())) {
                    String helperMessage = "Sure. Please fill the settle up form below with group, payer, receiver, and amount.";
                    saveMessage(conversation, AssistantMessageRole.USER, request.getMessage());
                    saveMessage(conversation, AssistantMessageRole.ASSISTANT, helperMessage);
                    return AssistantChatResponse.builder()
                        .conversationId(conversationId)
                        .message(helperMessage)
                        .requiresConfirmation(false)
                        .confirmationToken(null)
                        .actionType(ACTION_SETTLE_UP_FORM)
                        .actionData(Map.of())
                        .build();
                }

        saveMessage(conversation, AssistantMessageRole.USER, request.getMessage());

        String prompt = buildPromptWithHistory(conversation, request.getMessage());
        String output = generateAssistantReply(conversationId, userEmail, request.getMessage().length(), prompt);

        saveMessage(conversation, AssistantMessageRole.ASSISTANT, output);

        return AssistantChatResponse.builder()
                .conversationId(conversationId)
                .message(output)
                .requiresConfirmation(false)
                .confirmationToken(null)
                .actionType(null)
                .actionData(null)
                .build();
    }

    private AssistantChatResponse handleCreateExpenseSubmit(
            AssistantConversation conversation,
            String conversationId,
            String userEmail,
            AssistantChatRequest request
    ) {
        AssistantChatRequest.CreateExpenseAction createExpense = request.getCreateExpense();
        if (createExpense == null) {
            throw new RuntimeException("createExpense payload is required for CREATE_EQUAL_EXPENSE_SUBMIT action");
        }

        if (createExpense.getGroupId() == null || createExpense.getGroupId() <= 0) {
            throw new RuntimeException("Valid group selection is required");
        }
        if (createExpense.getPaidByUserId() == null || createExpense.getPaidByUserId() <= 0) {
            throw new RuntimeException("Valid payer selection is required");
        }
        if (!StringUtils.hasText(createExpense.getDescription())) {
            throw new RuntimeException("Expense description is required");
        }
        if (createExpense.getAmount() == null || createExpense.getAmount() <= 0) {
            throw new RuntimeException("Expense amount must be greater than 0");
        }
        if (!StringUtils.hasText(createExpense.getSplitType())) {
            throw new RuntimeException("Split type is required");
        }

        String splitType = createExpense.getSplitType().trim().toUpperCase();
        if (!"EQUAL".equals(splitType)) {
            throw new RuntimeException("Assistant currently supports only EQUAL split in guided mode.");
        }

        saveMessage(conversation, AssistantMessageRole.USER, "Create expense request submitted from guided form.");

        PendingAssistantAction pending = pendingActionService.prepareCreateEqualExpense(
                userEmail,
                createExpense.getGroupId(),
                createExpense.getPaidByUserId(),
                createExpense.getDescription().trim(),
                java.math.BigDecimal.valueOf(createExpense.getAmount()),
                splitType
        );

        String message = "I prepared your expense draft: " + pending.getSummary()
                + "\nPlease review and confirm to create it.";
        saveMessage(conversation, AssistantMessageRole.ASSISTANT, message);

        return AssistantChatResponse.builder()
                .conversationId(conversationId)
                .message(message)
                .requiresConfirmation(true)
                .confirmationToken(pending.getToken())
                .actionType(null)
                .actionData(null)
                .build();
    }

    private boolean isCreateExpenseIntent(String message) {
        String normalized = message.trim().toLowerCase();
        return normalized.contains("create expense")
                || normalized.contains("add expense")
                || normalized.contains("record expense")
                || normalized.contains("can you create expense")
            || normalized.contains("can you create expenses")
            || normalized.contains("prepare expense")
            || normalized.contains("prepare expense action")
            || normalized.contains("prepare expense actions")
            || normalized.contains("expense action")
            || normalized.contains("expense actions")
            || normalized.contains("open expense form")
            || normalized.contains("show expense form")
            || normalized.contains("new expense");
    }

    private boolean isDirectBalanceIntent(String message) {
        String normalized = message.trim().toLowerCase();
        return normalized.contains("check balances")
            || normalized.contains("check balance")
            || normalized.contains("my balance")
            || normalized.contains("balance")
            || normalized.contains("dues")
            || normalized.contains("due")
            || normalized.contains("who owes")
            || normalized.contains("what do i owe")
            || normalized.contains("what i owe")
            || normalized.contains("recent expenses")
            || normalized.contains("recent expense")
            || normalized.contains("latest expense")
            || normalized.contains("last expense")
            || normalized.contains("expense i have made")
            || normalized.contains("expenses i have made")
            || normalized.contains("expenses i made")
            || normalized.contains("expense i made");
    }

    private boolean isAssistantHelpIntent(String message) {
        String normalized = message.trim().toLowerCase();
        return normalized.equals("hi")
            || normalized.contains("help")
            || normalized.contains("what can you do")
            || normalized.contains("capabilities")
            || normalized.contains("services")
            || normalized.contains("features")
            || normalized.contains("assistant") && normalized.contains("do");
    }

    private boolean isFinancialAnalysisIntent(String message) {
        String normalized = message.trim().toLowerCase();
        return normalized.contains("summarize")
            || normalized.contains("summary")
            || normalized.contains("insight")
            || normalized.contains("insights")
            || normalized.contains("analyze")
            || normalized.contains("analysis")
            || normalized.contains("trend")
            || normalized.contains("trends")
            || normalized.contains("patterns")
            || normalized.contains("overview")
            || normalized.contains("spending habits");
    }

    private boolean isSettleUpIntent(String message) {
        String normalized = message.toLowerCase();
        return normalized.contains("settle up")
                || normalized.contains("settlement")
                || normalized.contains("settle balance")
                || normalized.contains("pay back")
                || normalized.contains("clear dues");
    }

        private boolean isLastQuestionIntent(String message) {
        String normalized = message.trim().toLowerCase();
        return normalized.contains("last question")
            || normalized.contains("previous question")
            || normalized.contains("what did i ask")
            || normalized.contains("earlier question");
        }

    private boolean isSessionInfoIntent(String message) {
        String normalized = message.trim().toLowerCase();
        return normalized.contains("session")
            || normalized.contains("conversation")
            || normalized.contains("my past chats")
            || normalized.contains("show my session")
            || normalized.contains("session summary")
            || normalized.contains("conversation summary")
            || normalized.contains("what did i do")
            || normalized.contains("what have i asked");
    }

    private AssistantChatResponse handleSessionInfoRequest(AssistantConversation conversation, String conversationId, String userEmail) {
        long messageCount = assistantChatMessageRepository.countByConversation(conversation);
        List<AssistantChatMessage> recent = assistantChatMessageRepository.findByConversationOrderByCreatedAtDesc(conversation, PageRequest.of(0, 5));
        String lastUser = recent.stream()
                .filter(m -> m.getRole() == AssistantMessageRole.USER)
                .map(AssistantChatMessage::getContent)
                .findFirst()
                .orElse("(no recent user messages)");

        String summary = String.format("Session id=%s. Messages=%d. Last user message: %s", conversationId, messageCount, lastUser);
        saveMessage(conversation, AssistantMessageRole.ASSISTANT, summary);

        return AssistantChatResponse.builder()
                .conversationId(conversationId)
                .message(summary)
                .requiresConfirmation(false)
                .confirmationToken(null)
                .actionType(null)
                .actionData(null)
                .build();
    }

        private AssistantChatResponse handleLastQuestionRequest(
            AssistantConversation conversation,
            String conversationId,
            String userEmail,
            String currentMessage
        ) {
        List<AssistantChatMessage> recentUserMessages = assistantChatMessageRepository
            .findByConversationUserEmailAndRoleOrderByCreatedAtDesc(
                userEmail,
                AssistantMessageRole.USER,
                PageRequest.of(0, LAST_QUESTION_LOOKBACK)
            );

        List<String> candidates = recentUserMessages.stream()
            .map(AssistantChatMessage::getContent)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .filter(content -> !content.equalsIgnoreCase(currentMessage.trim()))
            .filter(content -> !content.startsWith("CONFIRM_ACTION token="))
            .filter(content -> !content.equalsIgnoreCase("Create expense request submitted from guided form."))
            .filter(content -> !content.equalsIgnoreCase("Settle up request submitted from guided form."))
            .collect(Collectors.toList());

        String responseText;
        if (candidates.isEmpty()) {
            responseText = "I could not find any previous question in your saved chat history yet.";
        } else {
            responseText = "Your previous question from saved history was: \"" + candidates.get(0) + "\"";
        }

        saveMessage(conversation, AssistantMessageRole.USER, currentMessage);
        saveMessage(conversation, AssistantMessageRole.ASSISTANT, responseText);

        return AssistantChatResponse.builder()
            .conversationId(conversationId)
            .message(responseText)
            .requiresConfirmation(false)
            .confirmationToken(null)
            .actionType(null)
            .actionData(null)
            .build();
        }

        private boolean isToolUseFailure(RuntimeException ex) {
        String msg = ex.getMessage();
        if (!StringUtils.hasText(msg)) {
            return false;
        }
        String normalized = msg.toLowerCase();
        return normalized.contains("tool_use_failed")
            || normalized.contains("failed to call a function")
            || normalized.contains("failed_generation");
        }

    private AssistantChatResponse handleSettleUpSubmit(
            AssistantConversation conversation,
            String conversationId,
            String userEmail,
            AssistantChatRequest request
    ) {
        AssistantChatRequest.SettleUpAction settleUp = request.getSettleUp();
        if (settleUp == null) {
            throw new RuntimeException("settleUp payload is required for SETTLE_UP_SUBMIT action");
        }

        if (settleUp.getGroupId() == null || settleUp.getGroupId() <= 0) {
            throw new RuntimeException("Valid group selection is required");
        }
        if (settleUp.getPayerId() == null || settleUp.getPayerId() <= 0) {
            throw new RuntimeException("Valid payer selection is required");
        }
        if (settleUp.getReceiverId() == null || settleUp.getReceiverId() <= 0) {
            throw new RuntimeException("Valid receiver selection is required");
        }
        if (settleUp.getPayerId().equals(settleUp.getReceiverId())) {
            throw new RuntimeException("Payer and receiver cannot be the same person");
        }
        if (settleUp.getAmount() == null || settleUp.getAmount() <= 0) {
            throw new RuntimeException("Settlement amount must be greater than 0");
        }

        saveMessage(conversation, AssistantMessageRole.USER, "Settle up request submitted from guided form.");

        PendingAssistantAction pending = pendingActionService.prepareSettleUp(
                userEmail,
                settleUp.getGroupId(),
                settleUp.getPayerId(),
                settleUp.getReceiverId(),
                java.math.BigDecimal.valueOf(settleUp.getAmount())
        );

        String message = "I prepared your settlement draft: " + pending.getSummary()
                + "\nPlease review and confirm to execute it.";
        saveMessage(conversation, AssistantMessageRole.ASSISTANT, message);

        return AssistantChatResponse.builder()
                .conversationId(conversationId)
                .message(message)
                .requiresConfirmation(true)
                .confirmationToken(pending.getToken())
                .actionType(null)
                .actionData(null)
                .build();
    }

    private boolean isRateLimitFailure(RuntimeException ex) {
        String msg = ex.getMessage();
        if (!StringUtils.hasText(msg)) {
            return false;
        }
        String normalized = msg.toLowerCase();
        return normalized.contains("rate limit")
                || normalized.contains("rate_limit_exceeded")
                || normalized.contains("tokens per minute")
                || normalized.contains("tpm");
    }

    private String buildRateLimitMessage(RuntimeException ex) {
        String msg = ex.getMessage();
        if (!StringUtils.hasText(msg)) {
            return "Assistant is temporarily rate-limited. Please wait a few seconds and try again.";
        }

        Matcher matcher = RETRY_SECONDS_PATTERN.matcher(msg);
        if (matcher.find()) {
            return "Assistant is rate-limited right now. Please retry in about " + matcher.group(1) + " seconds.";
        }

        return "Assistant is temporarily rate-limited. Please wait a few seconds and try again.";
    }

    private AssistantChatResponse handleDirectBalanceRequest(
            AssistantConversation conversation,
            String conversationId,
            String userMessage,
            String userEmail
    ) {
        saveMessage(conversation, AssistantMessageRole.USER, userMessage);

        AssistantUserContext.setUserEmail(userEmail);
        String responseText;
        try {
            String summary = splitwiseAssistantTools.getMyFinancialSummary();
            String dues = splitwiseAssistantTools.getWhoOwesWhom();
            String recent = splitwiseAssistantTools.getRecentExpenses(5);

            responseText = summary + "\n\n" + dues + "\n\n" + recent;
        } finally {
            AssistantUserContext.clear();
        }

        saveMessage(conversation, AssistantMessageRole.ASSISTANT, responseText);

        return AssistantChatResponse.builder()
                .conversationId(conversationId)
                .message(responseText)
                .requiresConfirmation(false)
                .confirmationToken(null)
                .actionType(null)
                .actionData(null)
                .build();
    }

            private AssistantChatResponse handleAssistantHelpRequest(
                AssistantConversation conversation,
                String conversationId,
                String userMessage
            ) {
            saveMessage(conversation, AssistantMessageRole.USER, userMessage);
            String responseText = buildAssistantCapabilitiesResponse();
            saveMessage(conversation, AssistantMessageRole.ASSISTANT, responseText);
            return AssistantChatResponse.builder()
                .conversationId(conversationId)
                .message(responseText)
                .requiresConfirmation(false)
                .confirmationToken(null)
                .actionType(null)
                .actionData(null)
                .build();
            }

            private String buildAssistantCapabilitiesResponse() {
            return "Here\'s what I can help you with:\n\n"
                + "Spending & Balances\n"
                + "• Check who owes whom\n"
                + "• View recent expenses\n"
                + "• Get spending summaries\n\n"
                + "Expense Management\n"
                + "• Create shared expenses\n"
                + "• Update expense details\n"
                + "• Prepare settlements\n\n"
                + "Insights & Analytics\n"
                + "• Analyze spending patterns\n"
                + "• Generate spending insights\n"
                + "• Summarize financial activity";
            }

    private AssistantChatResponse handleFinancialAnalysisRequest(
            AssistantConversation conversation,
            String conversationId,
            String userEmail,
            String userMessage
    ) {
        saveMessage(conversation, AssistantMessageRole.USER, userMessage);

        AssistantUserContext.setUserEmail(userEmail);
        String contextJson;
        String balances;
        String dues;
        try {
            contextJson = splitwiseAssistantTools.getFinancialContext();
            balances = splitwiseAssistantTools.getMyFinancialSummary();
            dues = splitwiseAssistantTools.getWhoOwesWhom();
        } finally {
            AssistantUserContext.clear();
        }

        String prompt = "User request: " + userMessage + "\n"
                + "Financial context (JSON): " + contextJson + "\n"
                + "Balance summary: " + balances + "\n"
                + "Dues summary: " + dues + "\n"
                + "Instructions: Provide concise insights, patterns, or observations. "
                + "Do not repeat raw expense lists. Use bullet points when helpful.";

        String output = generateAssistantReply(conversationId, userEmail, userMessage.length(), prompt);
        saveMessage(conversation, AssistantMessageRole.ASSISTANT, output);

        return AssistantChatResponse.builder()
                .conversationId(conversationId)
                .message(output)
                .requiresConfirmation(false)
                .confirmationToken(null)
                .actionType(null)
                .actionData(null)
                .build();
    }

    private String generateAssistantReply(String conversationId, String userEmail, int inputChars, String prompt) {
        SplitwiseChatAgent agent = getAgent();
        if (agent == null) {
            throw new RuntimeException(
                    "Assistant model is not configured. Set assistant.api-key or langchain4j.open-ai.chat-model.api-key (or GROQ_API_KEY)."
            );
        }

        log.info(
                "Assistant chat request: conversationId={}, user={}, inputChars={}, promptChars={}, historyLimit={}, maxOutputTokens={}",
                conversationId,
                userEmail,
                inputChars,
                prompt.length(),
                assistantProperties.getHistoryLoadLimit(),
                resolveMaxOutputTokens()
        );

        AssistantUserContext.setUserEmail(userEmail);
        try {
            return agent.chat(userEmail, prompt);
        } catch (RuntimeException ex) {
            if (isToolUseFailure(ex)) {
                log.warn("Tool-call generation failed, retrying with plain chat model. conversationId={}, user={}", conversationId, userEmail, ex);
                SplitwisePlainAgent plainAgent = getPlainAgent();
                if (plainAgent != null) {
                    return plainAgent.chat(userEmail, prompt);
                }
                throw ex;
            }
            if (isRateLimitFailure(ex)) {
                throw new RuntimeException(buildRateLimitMessage(ex));
            }
            throw ex;
        } finally {
            AssistantUserContext.clear();
        }
    }

    private AssistantConversation getOrCreateConversation(String userEmail, String conversationId) {
        return assistantConversationRepository.findByIdAndUserEmail(conversationId, userEmail)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    LocalDateTime now = LocalDateTime.now();
                    AssistantConversation conversation = AssistantConversation.builder()
                            .id(conversationId)
                            .user(user)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return assistantConversationRepository.save(conversation);
                });
    }

    private void saveMessage(AssistantConversation conversation, AssistantMessageRole role, String content) {
        AssistantChatMessage message = AssistantChatMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        assistantChatMessageRepository.save(message);

        conversation.setUpdatedAt(LocalDateTime.now());
        assistantConversationRepository.save(conversation);
    }

    private String buildPromptWithHistory(AssistantConversation conversation, String currentUserMessage) {
        int historyLimit = Math.max(assistantProperties.getHistoryLoadLimit(), 0);
        if (historyLimit == 0) {
            return truncate(currentUserMessage, MAX_HISTORY_MESSAGE_CHARS);
        }

        List<AssistantChatMessage> recent = assistantChatMessageRepository.findByConversationOrderByCreatedAtDesc(
                conversation,
                PageRequest.of(0, historyLimit + 1)
        );

        if (recent.isEmpty()) {
            return truncate(currentUserMessage, MAX_HISTORY_MESSAGE_CHARS);
        }

        List<AssistantChatMessage> ascending = new ArrayList<>(recent);
        ascending.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

        // Exclude the just-persisted current user message to avoid duplicated prompt content.
        if (!ascending.isEmpty()) {
            AssistantChatMessage last = ascending.get(ascending.size() - 1);
            if (last.getRole() == AssistantMessageRole.USER && Objects.equals(last.getContent(), currentUserMessage)) {
                ascending.remove(ascending.size() - 1);
            }
        }

        if (ascending.isEmpty()) {
            return truncate(currentUserMessage, MAX_HISTORY_MESSAGE_CHARS);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Authoritative conversation transcript from previous turns (oldest to newest):\n");
        for (AssistantChatMessage message : ascending) {
            String safeContent = truncate(message.getContent(), MAX_HISTORY_MESSAGE_CHARS);
            sb.append(message.getRole().name()).append(": ").append(safeContent).append("\n");
            if (sb.length() >= MAX_TOTAL_PROMPT_CHARS) {
                break;
            }
        }
        sb.append("Current user message: ").append(truncate(currentUserMessage, MAX_HISTORY_MESSAGE_CHARS)).append("\n");
        sb.append("Important: Use the transcript above as memory. Do not say you cannot access prior context unless transcript is empty.");

        String prompt = sb.toString();
        if (prompt.length() <= MAX_TOTAL_PROMPT_CHARS) {
            return prompt;
        }

        return prompt.substring(prompt.length() - MAX_TOTAL_PROMPT_CHARS);
    }

    private String truncate(String value, int maxChars) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
    }

    interface SplitwiseChatAgent {

        @SystemMessage("""
            You are the Splitwise Assistant — an autonomous, user-friendly financial assistant for a shared expense app.
            Always act autonomously: when a request requires data, call the appropriate tools yourself and answer with the results.
            Never expose tool syntax, internal reasoning, planning notes, or backend details to the user.
            If a user asks for spending summaries, recent expenses, analytics, totals, balances, category insights, group expenses, or trends, fetch data first, then respond.
            For insights, trends, observations, patterns, analysis, summaries, or recommendations, do not repeat raw expense lists. Instead, provide concise, human-readable insights derived from the data.
            Highlight balances, dominant categories/groups, unusual spending, recurring expenses, or debt patterns when relevant.
            Use concise, natural responses with clear bullet points when helpful.
            For write operations, follow the prepare+confirm pattern: prepare a pending action, return a confirmation token, and require explicit confirmation before executing.
            Do not fabricate exact balances, users, groups, or expenses. If data is unavailable after tool calls, explain what is missing and suggest a helpful next step.
            Respect privacy: never access or act on other users' data without explicit consent.
            Use provided transcript as memory and keep responses focused on the user's request.
            """)
        String chat(@MemoryId String conversationId, @UserMessage String message);
    }

    interface SplitwisePlainAgent {

        @SystemMessage("""
            You are the Splitwise Assistant for a shared expense app. Be concise, proactive, and user-friendly.
            Use tools for any user-specific data and never expose tool syntax or internal reasoning.
            If data is unavailable after tool calls, explain what is missing and suggest the next helpful action.
            Require explicit confirmation before executing any write operation.
            """)
        String chat(@MemoryId String conversationId, @UserMessage String message);
    }
}
