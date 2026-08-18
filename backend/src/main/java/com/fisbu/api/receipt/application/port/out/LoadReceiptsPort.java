package com.fisbu.api.receipt.application.port.out;

import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

public interface LoadReceiptsPort {

    // En yeni önce sıralı, en fazla `limit` kayıt döner (bkz. ReceiptService.MAX_RECEIPTS_LIST)
    List<Receipt> loadByUserId(Long userId, int limit);
}
