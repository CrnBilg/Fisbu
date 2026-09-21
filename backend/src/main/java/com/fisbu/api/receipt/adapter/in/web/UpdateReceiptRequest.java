package com.fisbu.api.receipt.adapter.in.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateReceiptRequest {

    @NotBlank(message = "Mağaza adı boş olamaz")
    @Size(min = 1, max = 100, message = "Mağaza adı 1-100 karakter arasında olmalıdır")
    private String storeName;

    @NotNull(message = "Tutar boş olamaz")
    @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
    @DecimalMax(value = "999999.99", message = "Tutar 999.999,99 TL'yi geçemez")
    private BigDecimal totalAmount;

    @NotNull(message = "Tarih boş olamaz")
    private LocalDate receiptDate;

    private Long categoryId;

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
