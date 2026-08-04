package com.fisbu.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HouseholdResponse {
    private Long id;
    private String name;
    private String inviteCode;
    private List<HouseholdMemberResponse> members;
}
