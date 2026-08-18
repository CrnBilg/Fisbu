package com.fisbu.api.receipt.application.port.in;

import java.time.LocalDate;

import com.fisbu.api.receipt.domain.Receipt;

public interface SetReceiptRemindersUseCase {

    Receipt setReminders(String email, Long receiptId, LocalDate returnDeadline, LocalDate warrantyExpiryDate);
}
