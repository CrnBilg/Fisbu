package com.fisbu.api.receipt.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReceiptRequest {

    @NotBlank(message = "Mağaza adı boş olamaz")
    @Size(min = 1, max = 100, message = "Mağaza adı 1-100 karakter arasında olmalıdır")
    private String storeName;

    @NotNull(message = "Tutar boş olamaz")
    @DecimalMin(value = "0.01", message = "Tutar 0'dan büyük olmalıdır")
    @DecimalMax(value = "999999.99", message = "Tutar 999.999,99 TL'yi geçemez")
    private BigDecimal totalAmount;

    @NotNull(message = "Tarih boş olamaz")
    private LocalDate receiptDate;

    private String imageUrl;
    private String rawOcrText;
    private Long categoryId;

    // Garanti/iade hatırlatıcı — opsiyonel, ikisi de gönderilmezse hatırlatma kurulmaz
    private LocalDate returnDeadline;
    private LocalDate warrantyExpiryDate;

    // true ise aynı mağaza+tutar+tarih eşleşmesi olsa bile fiş yine de kaydedilir
    // (kullanıcı "yine de ekle" uyarısını onayladıysa)
    private boolean allowDuplicate;

    @Valid
    private List<ReceiptItemRequest> items;

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getRawOcrText() { return rawOcrText; }
    public void setRawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public LocalDate getReturnDeadline() { return returnDeadline; }
    public void setReturnDeadline(LocalDate returnDeadline) { this.returnDeadline = returnDeadline; }
    public LocalDate getWarrantyExpiryDate() { return warrantyExpiryDate; }
    public void setWarrantyExpiryDate(LocalDate warrantyExpiryDate) { this.warrantyExpiryDate = warrantyExpiryDate; }
    public boolean isAllowDuplicate() { return allowDuplicate; }
    public void setAllowDuplicate(boolean allowDuplicate) { this.allowDuplicate = allowDuplicate; }
    public List<ReceiptItemRequest> getItems() { return items; }
    public void setItems(List<ReceiptItemRequest> items) { this.items = items; }
}
