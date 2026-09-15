package com.fisbu.api.receipt.application.port.out;

import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

/**
 * ARCH-003/ADR-005: kullanıcının TÜM (limitsiz, tüm zamanlar) fişlerini yükler —
 * {@code LoadReceiptsPort.loadByUserId(userId, limit)}'ten farklı olarak, tüm geçmişi
 * analiz eden istatistik hesaplamaları (mağaza özeti, abonelik tespiti, harcama kişiliği) için.
 */
public interface LoadAllReceiptsByUserPort {

    List<Receipt> loadAllByUserId(Long userId);
}
