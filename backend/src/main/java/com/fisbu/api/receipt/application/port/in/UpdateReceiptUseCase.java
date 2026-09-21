package com.fisbu.api.receipt.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fisbu.api.receipt.domain.Receipt;

public interface UpdateReceiptUseCase {

    // OCR yanlış okuduysa ya da kullanıcı bir yazım hatası fark ettiyse, fişi silip
    // baştan eklemek yerine çekirdek alanlarını (mağaza/tutar/tarih/kategori) düzeltebilmesi için
    Receipt updateReceipt(String email, Long receiptId, String storeName, BigDecimal totalAmount,
                           LocalDate receiptDate, Long categoryId);
}
