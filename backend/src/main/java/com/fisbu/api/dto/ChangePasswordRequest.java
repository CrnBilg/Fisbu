package com.fisbu.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Mevcut şifre boş olamaz")
    private String currentPassword;

    @NotBlank(message = "Yeni şifre boş olamaz")
    @Size(min = 8, message = "Yeni şifre en az 8 karakter olmalıdır")
    @Pattern(regexp = ".*\\d.*", message = "Yeni şifre en az bir rakam içermelidir")
    @Pattern(regexp = ".*[^a-zA-Z0-9].*", message = "Yeni şifre en az bir özel karakter içermelidir")
    private String newPassword;
}
