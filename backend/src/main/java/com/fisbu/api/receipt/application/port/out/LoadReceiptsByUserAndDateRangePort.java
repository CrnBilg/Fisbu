package com.fisbu.api.receipt.application.port.out;

import java.time.LocalDate;
import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

/** ARCH-003/ADR-005: tekli kullanıcının belirli bir tarih aralığındaki fişlerini yükler. */
public interface LoadReceiptsByUserAndDateRangePort {

    List<Receipt> loadByUserIdAndDateRange(Long userId, LocalDate start, LocalDate end);
}
