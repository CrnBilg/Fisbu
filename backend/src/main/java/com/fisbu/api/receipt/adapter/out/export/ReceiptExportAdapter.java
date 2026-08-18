package com.fisbu.api.receipt.adapter.out.export;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fisbu.api.entity.User;
import com.fisbu.api.receipt.application.port.out.GenerateReceiptExportPort;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.ExportService;

// ExportService henüz hexagonal'a taşınmadığı için legacy entity.Receipt üzerinde çalışıyor.
// Bu adaptör o detayı tamamen gizler: application katmanı hiçbir zaman entity.Receipt görmez,
// sadece byte[] sonucu alır.
@Component
public class ReceiptExportAdapter implements GenerateReceiptExportPort {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final ExportService exportService;

    public ReceiptExportAdapter(ReceiptRepository receiptRepository, UserRepository userRepository,
                                 ExportService exportService) {
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.exportService = exportService;
    }

    @Override
    public byte[] toPdf(Long userId, LocalDate start, LocalDate end, String startLabel, String endLabel) {
        return exportService.toPdf(loadReceipts(userId, start, end), startLabel, endLabel);
    }

    @Override
    public byte[] toExcel(Long userId, LocalDate start, LocalDate end) {
        return exportService.toExcel(loadReceipts(userId, start, end));
    }

    @Override
    public byte[] toCsv(Long userId, LocalDate start, LocalDate end) {
        return exportService.toCsv(loadReceipts(userId, start, end));
    }

    private List<com.fisbu.api.entity.Receipt> loadReceipts(Long userId, LocalDate start, LocalDate end) {
        User user = userRepository.findById(userId).orElseThrow();
        return receiptRepository.findByUserAndReceiptDateBetween(user, start, end);
    }
}
