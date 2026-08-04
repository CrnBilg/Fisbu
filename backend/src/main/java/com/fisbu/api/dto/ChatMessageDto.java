package com.fisbu.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDto {
    // "user" ya da "assistant"
    @NotBlank
    private String role;

    @NotBlank
    private String content;
}
