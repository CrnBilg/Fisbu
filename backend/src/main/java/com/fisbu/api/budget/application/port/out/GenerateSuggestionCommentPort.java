package com.fisbu.api.budget.application.port.out;

// AI servisi henüz hexagonal'a taşınmadığı için geçici köprü port'u.
public interface GenerateSuggestionCommentPort {

    boolean isConfigured();

    String generateText(String prompt);
}
