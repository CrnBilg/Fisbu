package com.fisbu.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.ContributeRequest;
import com.fisbu.api.dto.SavingsGoalRequest;
import com.fisbu.api.dto.SavingsGoalResponse;
import com.fisbu.api.dto.SavingsGoalSuggestionResponse;
import com.fisbu.api.entity.SavingsGoal;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.SavingsGoalRepository;
import com.fisbu.api.repository.UserRepository;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushService;
    private final AiService aiService;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository, UserRepository userRepository,
                               PushNotificationService pushService, AiService aiService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
        this.pushService = pushService;
        this.aiService = aiService;
    }

    public SavingsGoalResponse createGoal(String email, SavingsGoalRequest request) {
        User user = getUserByEmail(email);

        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        goal.setName(request.getName().trim());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());

        return toResponse(savingsGoalRepository.save(goal));
    }

    public List<SavingsGoalResponse> getGoals(String email) {
        User user = getUserByEmail(email);
        return savingsGoalRepository.findByUser(user).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(SavingsGoalResponse::isAchieved)
                        .thenComparing(g -> g.getTargetDate() == null ? LocalDate.MAX : g.getTargetDate()))
                .collect(Collectors.toList());
    }

    public SavingsGoalResponse contribute(String email, Long goalId, ContributeRequest request) {
        SavingsGoal goal = getOwnedGoal(email, goalId);

        BigDecimal newAmount = goal.getCurrentAmount().add(request.getAmount());
        if (newAmount.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hedefteki tutar negatif olamaz");
        }
        goal.setCurrentAmount(newAmount);

        if (newAmount.compareTo(goal.getTargetAmount()) >= 0 && !Boolean.TRUE.equals(goal.getAchievedNotified())) {
            pushService.send(goal.getUser().getFcmToken(), "Tasarruf Hedefine Ulaştın!",
                    "\"" + goal.getName() + "\" hedefine ulaştın, tebrikler!");
            goal.setAchievedNotified(true);
        } else if (newAmount.compareTo(goal.getTargetAmount()) < 0) {
            // Para çekilip tekrar hedefin altına düşülürse, tekrar ulaşınca bildirilebilsin
            goal.setAchievedNotified(false);
        }

        return toResponse(savingsGoalRepository.save(goal));
    }

    public void deleteGoal(String email, Long goalId) {
        SavingsGoal goal = getOwnedGoal(email, goalId);
        savingsGoalRepository.delete(goal);
    }

    public SavingsGoalSuggestionResponse getSuggestion(String email, Long goalId) {
        SavingsGoal goal = getOwnedGoal(email, goalId);
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());

        SavingsGoalSuggestionResponse response = new SavingsGoalSuggestionResponse();

        if (remaining.signum() <= 0) {
            response.setComment("\"" + goal.getName() + "\" hedefine zaten ulaştın, tebrikler!");
            return response;
        }

        if (goal.getTargetDate() == null) {
            response.setComment("Bu hedef için bir tarih belirlersen, ayda ne kadar biriktirmen "
                    + "gerektiğini hesaplayabilirim.");
            return response;
        }

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
        long monthsLeft = Math.max(1, Math.round(daysLeft / 30.0));
        BigDecimal requiredMonthly = remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.UP);
        response.setRequiredMonthlyContribution(requiredMonthly);
        response.setComment(buildSuggestionComment(goal, remaining, requiredMonthly, monthsLeft));
        return response;
    }

    private String buildSuggestionComment(SavingsGoal goal, BigDecimal remaining, BigDecimal requiredMonthly,
                                           long monthsLeft) {
        String fallback = "\"" + goal.getName() + "\" hedefine ulaşmak için kalan " + monthsLeft
                + " ayda ortalama " + requiredMonthly + " TL biriktirmen gerekiyor";

        if (!aiService.isConfigured()) {
            return fallback;
        }

        try {
            String prompt = """
                    Kullanıcının "%s" adında bir tasarruf hedefi var. Hedef tutar %s TL, şu ana kadar %s TL \
                    biriktirmiş, kalan %s TL. Hedefe %d ay kaldı, bu da ayda yaklaşık %s TL biriktirmesi \
                    gerektiği anlamına geliyor. Kullanıcıya bunu açıklayan, motive edici ve kısa (1-2 cümle), \
                    Türkçe bir not yaz. Sadece cümleleri yaz, başka açıklama ekleme. Yanıtın SADECE Türkçe \
                    olmalı, başka dilden tek kelime bile kullanma.
                    """.formatted(goal.getName(), goal.getTargetAmount(), goal.getCurrentAmount(), remaining,
                    monthsLeft, requiredMonthly);
            String comment = aiService.generateText(prompt).trim();
            return comment.isEmpty() ? fallback : comment;
        } catch (Exception e) {
            return fallback;
        }
    }

    private SavingsGoal getOwnedGoal(String email, Long goalId) {
        User user = getUserByEmail(email);
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hedef bulunamadı"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu hedefe erişim yetkiniz yok");
        }
        return goal;
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {
        SavingsGoalResponse response = new SavingsGoalResponse();
        response.setId(goal.getId());
        response.setName(goal.getName());
        response.setTargetAmount(goal.getTargetAmount());
        response.setCurrentAmount(goal.getCurrentAmount());
        response.setTargetDate(goal.getTargetDate());
        boolean achieved = goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0;
        response.setAchieved(achieved);
        double progress = goal.getTargetAmount().signum() > 0
                ? goal.getCurrentAmount().divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0;
        response.setProgressPercent(Math.min(100, Math.max(0, progress)));
        return response;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}
