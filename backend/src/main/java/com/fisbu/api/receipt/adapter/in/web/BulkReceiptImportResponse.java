package com.fisbu.api.receipt.adapter.in.web;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkReceiptImportResponse {
    private List<ReceiptResponse> created;
    private List<BulkImportError> failed;
}
