package com.fisbu.api.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HouseholdStatisticsResponse {
    private int year;
    private int month;
    private BigDecimal totalAmount;
    private List<HouseholdMemberTotalResponse> byMember;
    private List<HouseholdCategoryTotalResponse> byCategory;
}
