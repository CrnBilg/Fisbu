package com.fisbu.api.receipt.application.port.out;

public interface SearchReceiptsPort {

    ReceiptPage search(Long userId, String query, Long categoryId, boolean uncategorized, int page, int size);
}
