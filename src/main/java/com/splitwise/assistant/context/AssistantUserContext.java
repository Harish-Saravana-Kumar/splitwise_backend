package com.splitwise.assistant.context;

public final class AssistantUserContext {

    private static final ThreadLocal<String> USER_EMAIL = new ThreadLocal<>();

    private AssistantUserContext() {
    }

    public static void setUserEmail(String email) {
        USER_EMAIL.set(email);
    }

    public static String getUserEmail() {
        return USER_EMAIL.get();
    }

    public static void clear() {
        USER_EMAIL.remove();
    }
}
