package com.fisbu.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;

    @NotBlank(message = "Kod boş olamaz")
    @Size(min = 6, max = 6, message = "Kod 6 haneli olmalıdır")
    private String code;

    @NotBlank(message = "Yeni şifre boş olamaz")
    @Size(min = 8, message = "Yeni şifre en az 8 karakter olmalıdır")
    @Pattern(regexp = ".*\\d.*", message = "Yeni şifre en az bir rakam içermelidir")
    private String newPassword;
}
