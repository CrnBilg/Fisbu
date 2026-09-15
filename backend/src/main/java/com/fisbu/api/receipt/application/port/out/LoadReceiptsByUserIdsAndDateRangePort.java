package com.fisbu.api.receipt.application.port.out;

import java.time.LocalDate;
import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

/** ARCH-003/ADR-005: birden fazla kullanıcının (household üyeleri) belirli bir tarih aralığındaki fişlerini yükler. */
public interface LoadReceiptsByUserIdsAndDateRangePort {

    List<Receipt> loadByUserIdsAndDateRange(List<Long> userIds, LocalDate start, LocalDate end);
}
