package com.fisbu.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    @NotBlank(message = "Mesaj boş olamaz")
    private String message;

    // Önceki tur(lar) — istemci taşır, backend hafıza tutmaz
    @Valid
    private List<ChatMessageDto> history;
}
