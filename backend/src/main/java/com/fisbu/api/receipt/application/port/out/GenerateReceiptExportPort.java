package com.fisbu.api.receipt.application.port.out;

import java.time.LocalDate;

// ExportService henüz hexagonal'a taşınmadığı ve entity.Receipt üzerinde çalıştığı için,
// bu port o legacy servisi ve fiş sorgusunu tamamen adaptör katmanının arkasına gizler —
// application katmanı hiçbir zaman entity.Receipt görmez (bkz. adapter/out/export).
public interface GenerateReceiptExportPort {

    byte[] toPdf(Long userId, LocalDate start, LocalDate end, String startLabel, String endLabel);

    byte[] toExcel(Long userId, LocalDate start, LocalDate end);

    byte[] toCsv(Long userId, LocalDate start, LocalDate end);
}
