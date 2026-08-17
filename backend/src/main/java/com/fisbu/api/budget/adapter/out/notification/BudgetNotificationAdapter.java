package com.fisbu.api.budget.adapter.out.notification;

import org.springframework.stereotype.Component;

import com.fisbu.api.budget.application.port.out.SendBudgetNotificationPort;
import com.fisbu.api.service.PushNotificationService;

@Component
public class BudgetNotificationAdapter implements SendBudgetNotificationPort {

    private final PushNotificationService pushNotificationService;

    public BudgetNotificationAdapter(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @Override
    public void send(String fcmToken, String title, String body) {
        pushNotificationService.send(fcmToken, title, body);
    }
}
