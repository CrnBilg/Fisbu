package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HouseholdMemberTotalResponse {
    private Long userId;
    private String name;
    private String email;
    private BigDecimal totalAmount;
}
