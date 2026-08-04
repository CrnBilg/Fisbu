package com.fisbu.api.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipts_user_date", columnList = "user_id, receipt_date"),
        @Index(name = "idx_receipts_category_id", columnList = "category_id")
})
@Getter
@Setter
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "raw_ocr_text", columnDefinition = "TEXT")
    private String rawOcrText;

    // Fiş bölüştürme (split bill) sonucu — katılımcı/tutar listesi JSON olarak saklanır
    @Column(name = "split_details_json", columnDefinition = "TEXT")
    private String splitDetailsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Garanti/iade hatırlatıcı — WarrantyReminderScheduler bu tarihler yaklaşınca push gönderir
    @Column(name = "return_deadline")
    private LocalDate returnDeadline;

    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;

    @Column(name = "return_reminder_sent", nullable = false, columnDefinition = "boolean default false")
    private Boolean returnReminderSent = false;

    @Column(name = "warranty_reminder_sent", nullable = false, columnDefinition = "boolean default false")
    private Boolean warrantyReminderSent = false;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReceiptItem> items = new ArrayList<>();
}