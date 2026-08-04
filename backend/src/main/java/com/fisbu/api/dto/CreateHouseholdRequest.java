package com.fisbu.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateHouseholdRequest {
    @NotBlank(message = "Aile adı boş olamaz")
    private String name;
}
