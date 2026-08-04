package com.fisbu.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationPrefsRequest {
    @NotNull
    private Boolean budgetWarningEnabled;

    @NotNull
    private Boolean budgetOverspendEnabled;
}
