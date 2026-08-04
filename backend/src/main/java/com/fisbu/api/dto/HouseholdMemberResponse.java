package com.fisbu.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HouseholdMemberResponse {
    private Long userId;
    private String name;
    private String email;
}
