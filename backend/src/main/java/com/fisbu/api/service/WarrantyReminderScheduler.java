package com.fisbu.api.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fisbu.api.entity.Receipt;
import com.fisbu.api.repository.ReceiptRepository;

/**
 * Her gün, iade süresi 1 gün içinde dolacak ya da garantisi 30 gün içinde sona erecek
 * fişler için birer defalık push bildirimi gönderir.
 */
@Component
public class WarrantyReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarrantyReminderScheduler.class);
    private static final int RETURN_REMINDER_WINDOW_DAYS = 1;
    private static final int WARRANTY_REMINDER_WINDOW_DAYS = 30;

    private final ReceiptRepository receiptRepository;
    private final PushNotificationService pushService;

    public WarrantyReminderScheduler(ReceiptRepository receiptRepository, PushNotificationService pushService) {
        this.receiptRepository = receiptRepository;
        this.pushService = pushService;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Istanbul")
    public void sendReminders() {
        if (!pushService.isConfigured()) {
            return;
        }

        LocalDate today = LocalDate.now();
        sendReturnReminders(today);
        sendWarrantyReminders(today);
    }

    private void sendReturnReminders(LocalDate today) {
        List<Receipt> receipts = receiptRepository.findByReturnDeadlineBetweenAndReturnReminderSentFalse(
                today, today.plusDays(RETURN_REMINDER_WINDOW_DAYS));
        for (Receipt receipt : receipts) {
            try {
                long daysLeft = ChronoUnit.DAYS.between(today, receipt.getReturnDeadline());
                String body = daysLeft <= 0
                        ? receipt.getStoreName() + " fişinin iade süresi bugün doluyor"
                        : receipt.getStoreName() + " fişinin iade süresi " + daysLeft + " gün içinde doluyor";
                pushService.send(receipt.getUser().getFcmToken(), "İade Süresi Yaklaşıyor", body);
                receipt.setReturnReminderSent(true);
                receiptRepository.save(receipt);
            } catch (Exception e) {
                log.error("Fiş #{} için iade hatırlatıcısı gönderilemedi: {}", receipt.getId(), e.getMessage());
            }
        }
    }

    private void sendWarrantyReminders(LocalDate today) {
        List<Receipt> receipts = receiptRepository.findByWarrantyExpiryDateBetweenAndWarrantyReminderSentFalse(
                today, today.plusDays(WARRANTY_REMINDER_WINDOW_DAYS));
        for (Receipt receipt : receipts) {
            try {
                long daysLeft = ChronoUnit.DAYS.between(today, receipt.getWarrantyExpiryDate());
                String body = daysLeft <= 0
                        ? receipt.getStoreName() + " fişinin garantisi bugün sona eriyor"
                        : receipt.getStoreName() + " fişinin garantisi " + daysLeft + " gün içinde sona eriyor";
                pushService.send(receipt.getUser().getFcmToken(), "Garanti Süresi Yaklaşıyor", body);
                receipt.setWarrantyReminderSent(true);
                receiptRepository.save(receipt);
            } catch (Exception e) {
                log.error("Fiş #{} için garanti hatırlatıcısı gönderilemedi: {}", receipt.getId(), e.getMessage());
            }
        }
    }
}
