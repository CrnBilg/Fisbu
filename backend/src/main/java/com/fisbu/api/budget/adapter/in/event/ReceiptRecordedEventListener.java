package com.fisbu.api.budget.adapter.in.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fisbu.api.budget.application.port.in.CheckBudgetThresholdUseCase;
import com.fisbu.api.receipt.domain.event.ReceiptRecordedEvent;

// ARCH-001/ADR-001: receipt modülü artık budget'ın in-port'unu doğrudan çağırmıyor,
// bunun yerine ReceiptRecordedEvent yayınlıyor. Budget modülü bu event'i burada dinleyip
// mevcut CheckBudgetThresholdUseCase'i (davranışı değişmedi) tetikliyor. Düz @EventListener
// (senkron, aynı thread/transaction) kullanılıyor — davranış öncekiyle aynı: fiş kaydedilince
// aynı istekte eşik kontrolü/push bildirimi tetiklenir.
@Component
public class ReceiptRecordedEventListener {

    private final CheckBudgetThresholdUseCase checkBudgetThresholdUseCase;

    public ReceiptRecordedEventListener(CheckBudgetThresholdUseCase checkBudgetThresholdUseCase) {
        this.checkBudgetThresholdUseCase = checkBudgetThresholdUseCase;
    }

    @EventListener
    public void onReceiptRecorded(ReceiptRecordedEvent event) {
        checkBudgetThresholdUseCase.checkAndNotify(event.userId(), event.categoryId(), event.year(), event.month());
    }
}
