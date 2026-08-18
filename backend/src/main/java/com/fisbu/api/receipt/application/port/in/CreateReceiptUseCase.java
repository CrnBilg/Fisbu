package com.fisbu.api.receipt.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

public interface CreateReceiptUseCase {

    CreateReceiptResult createReceipt(CreateReceiptCommand command);

    record CreateReceiptCommand(String email, String storeName, BigDecimal totalAmount, LocalDate receiptDate,
                                 String imageUrl, String rawOcrText, Long categoryId, LocalDate returnDeadline,
                                 LocalDate warrantyExpiryDate, boolean allowDuplicate,
                                 List<ReceiptItemCommand> items) {
    }

    record ReceiptItemCommand(String productName, BigDecimal unitPrice, BigDecimal quantity) {
    }

    // anomalyWarning kalıcı değil, sadece bu isteğin cevabında gösterilen bilgilendirici bir uyarı
    record CreateReceiptResult(Receipt receipt, String anomalyWarning) {
    }
}
