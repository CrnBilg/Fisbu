package com.fisbu.api.receipt.adapter.in.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReceiptItemRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    private String productName;

    @NotNull(message = "Birim fiyat boş olamaz")
    @DecimalMin(value = "0.01", message = "Birim fiyat 0'dan büyük olmalıdır")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.01", message = "Adet 0'dan büyük olmalıdır")
    private BigDecimal quantity;

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
