package com.fisbu.api.budget.application.port.out;

// Push bildirim altyapısı henüz hexagonal'a taşınmadığı için geçici köprü port'u.
public interface SendBudgetNotificationPort {

    void send(String fcmToken, String title, String body);
}
