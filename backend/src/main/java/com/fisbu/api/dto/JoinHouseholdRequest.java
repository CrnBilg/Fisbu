package com.fisbu.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinHouseholdRequest {
    @NotBlank(message = "Davet kodu boş olamaz")
    private String inviteCode;
}
