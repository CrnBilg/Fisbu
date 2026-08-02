package com.fisbu.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkReceiptImportResponse {
    private List<ReceiptResponse> created;
    private List<BulkImportError> failed;
}
