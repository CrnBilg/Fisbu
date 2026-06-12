package com.fisbu.api.dto;

import lombok.Getter;
import lombok.Setter;
// Kayit olurken email ve password bilgilerini tutacak bir DTO sinifi oluşturuyoruz.
// Bu sinif, kullanicidan gelen kayit isteğini temsil eder.
// Lombok kütüphanesini kullanarak getter ve setter metodlarini otomatik olarak oluşturuyoruz.

@Getter
@Setter
public class RegisterRequest {
    private String email;
    private String password;
}