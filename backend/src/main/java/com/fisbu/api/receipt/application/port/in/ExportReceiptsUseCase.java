package com.fisbu.api.receipt.application.port.in;

import java.time.LocalDate;

public interface ExportReceiptsUseCase {

    ExportResult exportReceipts(String email, String format, LocalDate start, LocalDate end);

    record ExportResult(byte[] content, String mediaType, String filename) {
    }
}
