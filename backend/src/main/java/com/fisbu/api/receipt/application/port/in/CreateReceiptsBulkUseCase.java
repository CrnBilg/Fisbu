package com.fisbu.api.receipt.application.port.in;

import java.util.List;

public interface CreateReceiptsBulkUseCase {

    BulkCreateResult createReceiptsBulk(String email, List<CreateReceiptUseCase.CreateReceiptCommand> commands);

    record BulkCreateResult(List<CreateReceiptUseCase.CreateReceiptResult> created, List<BulkImportError> failed) {
    }

    record BulkImportError(int index, String error) {
    }
}
