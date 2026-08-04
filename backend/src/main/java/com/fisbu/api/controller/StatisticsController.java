package com.fisbu.api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.fisbu.api.dto.MonthlyStatisticsResponse;
import com.fisbu.api.dto.StoreStatResponse;
import com.fisbu.api.dto.SubscriptionCandidateResponse;
import com.fisbu.api.dto.TopProductResponse;
import com.fisbu.api.service.StatisticsService;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/monthly")
    public MonthlyStatisticsResponse getMonthlyStatistics(@AuthenticationPrincipal UserDetails userDetails,
                                                           @RequestParam(required = false) Integer year,
                                                           @RequestParam(required = false) Integer month) {
        return statisticsService.getMonthlyStatistics(userDetails.getUsername(), year, month);
    }

    @GetMapping("/stores")
    public List<StoreStatResponse> getStoreStatistics(@AuthenticationPrincipal UserDetails userDetails) {
        return statisticsService.getStoreStatistics(userDetails.getUsername());
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> getTopProducts(@AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestParam(required = false) Integer limit) {
        return statisticsService.getTopProducts(userDetails.getUsername(), limit);
    }

    @GetMapping("/subscriptions")
    public List<SubscriptionCandidateResponse> getPotentialSubscriptions(@AuthenticationPrincipal UserDetails userDetails) {
        return statisticsService.getPotentialSubscriptions(userDetails.getUsername());
    }
}
