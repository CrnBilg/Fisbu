package com.fisbu.api.receipt.application.port.out;

// Category modülünün UnlinkReceiptsFromCategoryPort'u için gerçek implementasyonu sağlayan port
// (bkz. category/adapter/out/receipt/CategoryReceiptAdapter) — artık legacy ReceiptRepository'ye
// değil, buraya delege ediyor.
public interface UnlinkCategoryFromReceiptsPort {

    void unlinkCategoryFromReceipts(Long categoryId);
}
