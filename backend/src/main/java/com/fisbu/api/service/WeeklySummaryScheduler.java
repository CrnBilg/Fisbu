package com.fisbu.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * Her Pazartesi sabahı, önceki hafta (Pzt-Paz) harcama toplamını bir önceki haftayla
 * kıyaslayan bir push bildirimi gönderir — proaktif engagement için.
 */
@Component
public class WeeklySummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklySummaryScheduler.class);

    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;
    private final PushNotificationService pushService;

    public WeeklySummaryScheduler(UserRepository userRepository, ReceiptRepository receiptRepository,
                                   PushNotificationService pushService) {
        this.userRepository = userRepository;
        this.receiptRepository = receiptRepository;
        this.pushService = pushService;
    }

    @Scheduled(cron = "0 0 9 * * MON", zone = "Europe/Istanbul")
    public void sendWeeklySummaries() {
        if (!pushService.isConfigured()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastWeekStart = today.minusDays(7);
        LocalDate lastWeekEnd = today.minusDays(1);
        LocalDate prevWeekStart = today.minusDays(14);
        LocalDate prevWeekEnd = today.minusDays(8);

        List<User> users = userRepository.findByFcmTokenIsNotNull();
        log.info("Haftalık özet bildirimi gönderiliyor: {} kullanıcı", users.size());

        for (User user : users) {
            try {
                sendSummaryForUser(user, lastWeekStart, lastWeekEnd, prevWeekStart, prevWeekEnd);
            } catch (Exception e) {
                log.error("Haftalık özet gönderilemedi ({}): {}", user.getEmail(), e.getMessage());
            }
        }
    }

    private void sendSummaryForUser(User user, LocalDate lastWeekStart, LocalDate lastWeekEnd,
                                     LocalDate prevWeekStart, LocalDate prevWeekEnd) {
        BigDecimal lastWeekTotal = sumReceipts(user, lastWeekStart, lastWeekEnd);
        if (lastWeekTotal.compareTo(BigDecimal.ZERO) == 0) {
            return; // Hiç harcama yoksa bildirimle rahatsız etme
        }
        BigDecimal prevWeekTotal = sumReceipts(user, prevWeekStart, prevWeekEnd);

        String body = buildBody(lastWeekTotal, prevWeekTotal);
        pushService.send(user.getFcmToken(), "Haftalık Harcama Özetin", body);
    }

    private BigDecimal sumReceipts(User user, LocalDate start, LocalDate end) {
        BigDecimal total = BigDecimal.ZERO;
        for (Receipt receipt : receiptRepository.findByUserAndReceiptDateBetween(user, start, end)) {
            if (receipt.getTotalAmount() != null) {
                total = total.add(receipt.getTotalAmount());
            }
        }
        return total;
    }

    private String buildBody(BigDecimal lastWeekTotal, BigDecimal prevWeekTotal) {
        String lastWeekLabel = formatTl(lastWeekTotal);

        if (prevWeekTotal.compareTo(BigDecimal.ZERO) == 0) {
            return "Bu hafta " + lastWeekLabel + " harcadın.";
        }

        BigDecimal changePercent = lastWeekTotal.subtract(prevWeekTotal)
                .divide(prevWeekTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        int roundedPercent = changePercent.abs().setScale(0, RoundingMode.HALF_UP).intValue();

        if (roundedPercent == 0) {
            return "Bu hafta " + lastWeekLabel + " harcadın, geçen haftayla aynı seviyede.";
        }
        String direction = changePercent.signum() < 0 ? "az" : "fazla";
        return "Bu hafta " + lastWeekLabel + " harcadın, geçen haftaya göre %" + roundedPercent + " " + direction + ".";
    }

    private String formatTl(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.of("tr", "TR"));
        return nf.format(amount) + " TL";
    }
}
