package com.fisbu.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpendingPersonalityResponse {
    private SpendingPersonaResponse persona;
    private List<BadgeResponse> badges;
}
