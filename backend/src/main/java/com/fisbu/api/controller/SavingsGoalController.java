package com.fisbu.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.ContributeRequest;
import com.fisbu.api.dto.SavingsGoalRequest;
import com.fisbu.api.dto.SavingsGoalResponse;
import com.fisbu.api.dto.SavingsGoalSuggestionResponse;
import com.fisbu.api.service.SavingsGoalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsGoalResponse createGoal(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestBody @Valid SavingsGoalRequest request) {
        return savingsGoalService.createGoal(userDetails.getUsername(), request);
    }

    @GetMapping
    public List<SavingsGoalResponse> getGoals(@AuthenticationPrincipal UserDetails userDetails) {
        return savingsGoalService.getGoals(userDetails.getUsername());
    }

    @PutMapping("/{id}/contribute")
    public SavingsGoalResponse contribute(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long id,
                                           @RequestBody @Valid ContributeRequest request) {
        return savingsGoalService.contribute(userDetails.getUsername(), id, request);
    }

    @GetMapping("/{id}/suggestion")
    public SavingsGoalSuggestionResponse getSuggestion(@AuthenticationPrincipal UserDetails userDetails,
                                                         @PathVariable Long id) {
        return savingsGoalService.getSuggestion(userDetails.getUsername(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        savingsGoalService.deleteGoal(userDetails.getUsername(), id);
    }
}
