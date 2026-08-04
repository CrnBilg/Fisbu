package com.fisbu.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.CreateHouseholdRequest;
import com.fisbu.api.dto.HouseholdResponse;
import com.fisbu.api.dto.HouseholdStatisticsResponse;
import com.fisbu.api.dto.JoinHouseholdRequest;
import com.fisbu.api.service.HouseholdService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdResponse createHousehold(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody @Valid CreateHouseholdRequest request) {
        return householdService.createHousehold(userDetails.getUsername(), request);
    }

    @PostMapping("/join")
    public HouseholdResponse joinHousehold(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody @Valid JoinHouseholdRequest request) {
        return householdService.joinHousehold(userDetails.getUsername(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveHousehold(@AuthenticationPrincipal UserDetails userDetails) {
        householdService.leaveHousehold(userDetails.getUsername());
    }

    @GetMapping("/me")
    public HouseholdResponse getMyHousehold(@AuthenticationPrincipal UserDetails userDetails) {
        return householdService.getMyHousehold(userDetails.getUsername());
    }

    @GetMapping("/statistics")
    public HouseholdStatisticsResponse getStatistics(@AuthenticationPrincipal UserDetails userDetails,
                                                       @RequestParam(required = false) Integer year,
                                                       @RequestParam(required = false) Integer month) {
        return householdService.getStatistics(userDetails.getUsername(), year, month);
    }
}
