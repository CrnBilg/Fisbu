package com.fisbu.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestoreReceiptRequest {

    @NotBlank(message = "OCR metni boş olamaz")
    private String rawOcrText;
}
