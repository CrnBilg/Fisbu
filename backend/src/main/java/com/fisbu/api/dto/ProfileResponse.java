package com.fisbu.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileResponse {
    private String email;
    private String name;
    private String profileImageUrl;
    private String createdAt;
}
