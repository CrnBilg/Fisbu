package com.fisbu.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParsedStatementResponse {
    private String sourceType;
    private List<ImportedTransactionDto> transactions;
    private List<String> warnings;
}
