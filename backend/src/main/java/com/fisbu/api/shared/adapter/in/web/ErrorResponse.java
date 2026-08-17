package com.fisbu.api.shared.adapter.in.web;

import java.util.Map;

// Tüm hata response'ları için tutarlı şekil: details sadece alan bazlı validasyon hatalarında dolu olur.
public record ErrorResponse(String error, Map<String, String> details) {

    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, null);
    }

    public static ErrorResponse of(String error, Map<String, String> details) {
        return new ErrorResponse(error, details);
    }
}
