package com.fisbu.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkReceiptImportRequest {

    @NotEmpty(message = "İçe aktarılacak fiş listesi boş olamaz")
    @Valid
    private List<ReceiptRequest> receipts;
}
