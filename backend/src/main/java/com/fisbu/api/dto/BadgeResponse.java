package com.fisbu.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BadgeResponse {
    private String id;
    private String title;
    private String description;
    private boolean achieved;
}
