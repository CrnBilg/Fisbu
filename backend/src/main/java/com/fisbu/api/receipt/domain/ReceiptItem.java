package com.fisbu.api.receipt.domain;

import java.math.BigDecimal;

public record ReceiptItem(Long id, String productName, String normalizedName, BigDecimal unitPrice,
                           BigDecimal quantity) {
}
