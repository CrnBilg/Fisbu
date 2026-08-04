package com.fisbu.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationPrefsResponse {
    private boolean budgetWarningEnabled;
    private boolean budgetOverspendEnabled;
}
