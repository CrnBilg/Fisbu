package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavingsGoalSuggestionResponse {
    private BigDecimal requiredMonthlyContribution;
    private String comment;
}
