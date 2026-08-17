package com.fisbu.api.budget.application.port.out;

public record UserNotificationProfile(String fcmToken, boolean notifyBudgetWarning, boolean notifyBudgetOverspend) {
}
