package com.fisbu.api.receipt.application.port.out;

import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

public record ReceiptPage(List<Receipt> content, int page, int size, long totalElements, int totalPages,
                           boolean hasNext) {
}
