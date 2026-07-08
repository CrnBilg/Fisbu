package com.fisbu.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Kategori adı boş olamaz")
    @Size(min = 1, max = 50, message = "Kategori adı 1-50 karakter arasında olmalıdır")
    private String name;

    private String color;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}