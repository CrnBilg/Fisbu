package com.fisbu.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkImportError {
    private int index;
    private String error;

    public BulkImportError() {
    }

    public BulkImportError(int index, String error) {
        this.index = index;
        this.error = error;
    }
}
