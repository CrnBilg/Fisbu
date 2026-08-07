package com.fisbu.api.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.fisbu.api.entity.Receipt;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Gün 14/15: fiş listesini PDF/Excel/CSV olarak üretir.
 * Dosya diske yazılmaz — doğrudan bellekte üretilip response'ta stream edilir
 * (projede zaten kalıcı dosya depolama yok, görseller Cloudinary'de tutuluyor).
 */
@Service
public class ExportService {

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    static final String[] HEADERS = {"Mağaza", "Tutar (TL)", "Tarih", "Kategori"};

    public byte[] toCsv(List<Receipt> receipts) {
        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // Excel'in Türkçe karakterleri doğru okuması için UTF-8 BOM
        sb.append(String.join(",", HEADERS)).append("\r\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Receipt receipt : receipts) {
            BigDecimal amount = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : BigDecimal.ZERO;
            total = total.add(amount);
            sb.append(csvField(receipt.getStoreName())).append(',')
                    .append(csvField(amount.toPlainString())).append(',')
                    .append(csvField(receipt.getReceiptDate() != null ? receipt.getReceiptDate().format(DATE_FORMAT) : ""))
                    .append(',')
                    .append(csvField(receipt.getCategory() != null ? receipt.getCategory().getName() : ""))
                    .append("\r\n");
        }
        sb.append(csvField("TOPLAM")).append(",").append(csvField(total.toPlainString())).append(",,\r\n");

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String csvField(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    public byte[] toExcel(List<Receipt> receipts) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Fişler");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            BigDecimal total = BigDecimal.ZERO;
            for (Receipt receipt : receipts) {
                BigDecimal amount = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : BigDecimal.ZERO;
                total = total.add(amount);

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(receipt.getStoreName() != null ? receipt.getStoreName() : "");
                row.createCell(1).setCellValue(amount.doubleValue());
                row.createCell(2).setCellValue(
                        receipt.getReceiptDate() != null ? receipt.getReceiptDate().format(DATE_FORMAT) : "");
                row.createCell(3).setCellValue(
                        receipt.getCategory() != null ? receipt.getCategory().getName() : "");
            }

            Row totalRow = sheet.createRow(rowIndex);
            Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("TOPLAM");
            totalLabelCell.setCellStyle(headerStyle);
            totalRow.createCell(1).setCellValue(total.doubleValue());

            // autoSizeColumn() yerine sabit genişlikler kullanılıyor: autoSizeColumn AWT font
            // metriklerine ihtiyaç duyar ve headless/minimal JRE ortamında (ör. Railway) hata
            // fırlatıp export'u 500'e düşürebiliyordu.
            int[] columnWidths = {30 * 256, 14 * 256, 14 * 256, 20 * 256};
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, i < columnWidths.length ? columnWidths[i] : 15 * 256);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Excel export başarısız oldu", e);
        }
    }

    public byte[] toPdf(List<Receipt> receipts, String startLabel, String endLabel) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = loadTurkishFont();
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, 16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, 11, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(baseFont, 10, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font boldCellFont = new com.lowagie.text.Font(baseFont, 10, com.lowagie.text.Font.BOLD);

            Paragraph title = new Paragraph("FişBu - Fiş Raporu (" + startLabel + " - " + endLabel + ")", titleFont);
            title.setSpacingAfter(12);
            document.add(title);

            PdfPTable table = new PdfPTable(HEADERS.length);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f, 2f, 2.5f});

            for (String header : HEADERS) {
                PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell.setPadding(6);
                table.addCell(cell);
            }

            BigDecimal total = BigDecimal.ZERO;
            for (Receipt receipt : receipts) {
                BigDecimal amount = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : BigDecimal.ZERO;
                total = total.add(amount);

                addTextCell(table, receipt.getStoreName() != null ? receipt.getStoreName() : "-", cellFont);
                addTextCell(table, amount.toPlainString() + " TL", cellFont);
                addTextCell(table, receipt.getReceiptDate() != null ? receipt.getReceiptDate().format(DATE_FORMAT) : "-", cellFont);
                addTextCell(table, receipt.getCategory() != null ? receipt.getCategory().getName() : "-", cellFont);
            }

            PdfPCell totalLabelCell = new PdfPCell(new Paragraph("TOPLAM", boldCellFont));
            totalLabelCell.setColspan(1);
            totalLabelCell.setPadding(5);
            table.addCell(totalLabelCell);
            PdfPCell totalValueCell = new PdfPCell(new Paragraph(total.toPlainString() + " TL", boldCellFont));
            totalValueCell.setColspan(3);
            totalValueCell.setPadding(5);
            table.addCell(totalValueCell);

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("PDF export başarısız oldu", e);
        }
    }

    private void addTextCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    /** Türkçe karakterlerin (ş, ğ, ı, İ, ç, ö, ü) doğru görünmesi için Noto Sans'ı gömülü olarak yükler. */
    private BaseFont loadTurkishFont() throws DocumentException, IOException {
        try (InputStream fontStream = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            if (fontStream == null) {
                throw new IOException("NotoSans-Regular.ttf bulunamadı (src/main/resources/fonts/)");
            }
            byte[] fontBytes = fontStream.readAllBytes();
            return BaseFont.createFont("NotoSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, fontBytes, null);
        }
    }
}
