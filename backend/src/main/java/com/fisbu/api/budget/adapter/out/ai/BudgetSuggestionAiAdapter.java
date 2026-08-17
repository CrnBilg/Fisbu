package com.fisbu.api.budget.adapter.out.ai;

import org.springframework.stereotype.Component;

import com.fisbu.api.budget.application.port.out.GenerateSuggestionCommentPort;
import com.fisbu.api.service.AiService;

@Component
public class BudgetSuggestionAiAdapter implements GenerateSuggestionCommentPort {

    private final AiService aiService;

    public BudgetSuggestionAiAdapter(AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public boolean isConfigured() {
        return aiService.isConfigured();
    }

    @Override
    public String generateText(String prompt) {
        return aiService.generateText(prompt);
    }
}
