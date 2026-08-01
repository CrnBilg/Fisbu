package com.fisbu.api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.PersonalInflationResponse;
import com.fisbu.api.dto.ProductPriceHistoryResponse;
import com.fisbu.api.service.PersonalInflationService;

@RestController
@RequestMapping("/inflation")
public class InflationController {

    private final PersonalInflationService personalInflationService;

    public InflationController(PersonalInflationService personalInflationService) {
        this.personalInflationService = personalInflationService;
    }

    @GetMapping("/summary")
    public PersonalInflationResponse getSummary(@AuthenticationPrincipal UserDetails userDetails,
                                                 @RequestParam(required = false) Integer months) {
        return personalInflationService.getPersonalInflationSummary(userDetails.getUsername(), months);
    }

    @GetMapping("/products/{normalizedName}/history")
    public ProductPriceHistoryResponse getProductHistory(@AuthenticationPrincipal UserDetails userDetails,
                                                           @PathVariable String normalizedName) {
        return personalInflationService.getProductPriceHistory(userDetails.getUsername(), normalizedName);
    }
}
