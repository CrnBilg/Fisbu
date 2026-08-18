package com.fisbu.api.receipt.application.port.in;

import com.fisbu.api.receipt.application.port.out.ReceiptPage;

public interface SearchReceiptsUseCase {

    ReceiptPage searchReceipts(String email, String query, Long categoryId, boolean uncategorized, int page, int size);
}
