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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.BudgetRequest;
import com.fisbu.api.dto.BudgetResponse;
import com.fisbu.api.service.BudgetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> getBudgets(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestParam(required = false) Integer year,
                                            @RequestParam(required = false) Integer month) {
        return budgetService.getBudgets(userDetails.getUsername(), year, month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse createBudget(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody @Valid BudgetRequest request) {
        return budgetService.createBudget(userDetails.getUsername(), request);
    }

    @PutMapping("/{id}")
    public BudgetResponse updateBudget(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long id,
                                        @RequestBody @Valid BudgetRequest request) {
        return budgetService.updateBudget(userDetails.getUsername(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        budgetService.deleteBudget(userDetails.getUsername(), id);
    }
}
