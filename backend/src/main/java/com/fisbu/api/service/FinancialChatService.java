package com.fisbu.api.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fisbu.api.dto.BudgetResponse;
import com.fisbu.api.dto.ChatMessageDto;
import com.fisbu.api.dto.ChatRequest;
import com.fisbu.api.dto.ChatResponse;
import com.fisbu.api.dto.MonthlyStatisticsResponse;
import com.fisbu.api.dto.SavingsGoalResponse;

/**
 * Serbest metinli finansal asistan — "bu ay ne kadar tasarruf edebilirim" tarzı sorulara,
 * kullanıcının gerçek harcama/bütçe/tasarruf hedefi verisini bağlam olarak vererek yanıt üretir.
 * Konuşma geçmişi backend'de tutulmaz, her istekte istemci taşır.
 */
@Service
public class FinancialChatService {

    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int MONTHLY_HISTORY_SPAN = 6;
    private static final String[] TURKISH_MONTHS = {
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    };

    private final AiService aiService;
    private final StatisticsService statisticsService;
    private final BudgetService budgetService;
    private final SavingsGoalService savingsGoalService;

    public FinancialChatService(AiService aiService, StatisticsService statisticsService,
                                 BudgetService budgetService, SavingsGoalService savingsGoalService) {
        this.aiService = aiService;
        this.statisticsService = statisticsService;
        this.budgetService = budgetService;
        this.savingsGoalService = savingsGoalService;
    }

    public ChatResponse sendMessage(String email, ChatRequest request) {
        String systemPrompt = buildSystemPrompt(email);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (request.getHistory() != null) {
            List<ChatMessageDto> trimmed = request.getHistory();
            if (trimmed.size() > MAX_HISTORY_MESSAGES) {
                trimmed = trimmed.subList(trimmed.size() - MAX_HISTORY_MESSAGES, trimmed.size());
            }
            for (ChatMessageDto m : trimmed) {
                messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }
        }
        messages.add(Map.of("role", "user", "content", request.getMessage()));

        String reply = aiService.chatConversation(messages).trim();
        return new ChatResponse(reply);
    }

    private String buildSystemPrompt(String email) {
        LocalDate today = LocalDate.now();
        // Tek sorguda son MONTHLY_HISTORY_SPAN ayı çeker; son eleman (bu ay) hem kategori
        // dökümü hem de aylık geçmiş listesi için kullanılır — 7 ayrı sorgu yerine 1
        List<MonthlyStatisticsResponse> monthlyRange =
                statisticsService.getMonthlyStatisticsRange(email, MONTHLY_HISTORY_SPAN);
        MonthlyStatisticsResponse stats = monthlyRange.get(monthlyRange.size() - 1);

        String categoryBreakdown = stats.getCategories().stream()
                .map(c -> "- " + c.getCategoryName() + ": " + c.getTotalAmount() + " TL")
                .collect(Collectors.joining("\n"));
        if (categoryBreakdown.isBlank()) {
            categoryBreakdown = "(bu ay hiç fiş eklenmemiş)";
        }

        List<BudgetResponse> budgets = budgetService.getBudgets(email, today.getYear(), today.getMonthValue());
        String budgetSummary = budgets.isEmpty()
                ? "(bu ay bütçe tanımlanmamış)"
                : budgets.stream()
                        .map(b -> "- " + b.getCategoryName() + ": " + b.getCurrentSpend() + " / " + b.getMonthlyLimit()
                                + " TL" + (Boolean.TRUE.equals(b.getOverBudget()) ? " (AŞILDI)" : ""))
                        .collect(Collectors.joining("\n"));

        List<SavingsGoalResponse> goals = savingsGoalService.getGoals(email);
        String goalsSummary = goals.isEmpty()
                ? "(tasarruf hedefi yok)"
                : goals.stream()
                        .map(g -> "- " + g.getName() + ": " + g.getCurrentAmount() + " / " + g.getTargetAmount()
                                + " TL (%" + Math.round(g.getProgressPercent()) + ")")
                        .collect(Collectors.joining("\n"));

        String monthlyHistory = buildMonthlyHistory(monthlyRange);

        return """
                Sen FişBu uygulamasında kullanıcının kişisel finansal asistanısın. Kullanıcının %d yılı \
                %d ayı harcama durumu:

                Toplam harcama: %s TL
                Kategori bazında:
                %s

                Son %d aydaki toplam harcama geçmişi (kullanıcı belirli bir ayı sorarsa buradan yanıtla):
                %s
                Bu liste TAM ve KESİNDİR: bir ay 0 TL gösteriyorsa veri eksik değildir, o ay hiç \
                harcama yapılmamış demektir. Listedeki 6 aydan herhangi biri sorulduğunda "veri yok" \
                deme, doğrudan listedeki tutarla yanıtla. Sadece bu listenin DIŞINDAKİ bir ay sorulursa \
                o aya ait verin olmadığını söyle.

                Bütçeler:
                %s

                Tasarruf hedefleri:
                %s

                Kullanıcının sorularını bu gerçek verilere dayanarak yanıtla. Samimi, kısa (en fazla \
                3-4 cümle) ve pratik ol. Kesin yatırım/vergi/hukuki tavsiye verme — bunun yerine genel \
                gözlem ve öneri sun. Elindeki veriyle cevap veremiyorsan bunu açıkça söyle, uydurma. \
                Yanıtın SADECE Türkçe olmalı, başka dilden tek kelime bile kullanma, sadece Türk \
                alfabesindeki harfleri kullan (başka hiçbir yazı sistemi/alfabe kullanma).
                """.formatted(today.getYear(), today.getMonthValue(), stats.getTotalAmount(),
                categoryBreakdown, MONTHLY_HISTORY_SPAN, monthlyHistory, budgetSummary, goalsSummary);
    }

    private String buildMonthlyHistory(List<MonthlyStatisticsResponse> monthlyRange) {
        StringBuilder sb = new StringBuilder();
        for (MonthlyStatisticsResponse monthStats : monthlyRange) {
            sb.append("- ").append(TURKISH_MONTHS[monthStats.getMonth() - 1]).append(" ")
                    .append(monthStats.getYear()).append(": ").append(monthStats.getTotalAmount()).append(" TL\n");
        }
        return sb.toString();
    }
}
