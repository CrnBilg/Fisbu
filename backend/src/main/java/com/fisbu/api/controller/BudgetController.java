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

import com.fisbu.api.budget.application.port.in.CreateBudgetUseCase;
import com.fisbu.api.budget.application.port.in.DeleteBudgetUseCase;
import com.fisbu.api.budget.application.port.in.GetBudgetSuggestionUseCase;
import com.fisbu.api.budget.application.port.in.GetBudgetsUseCase;
import com.fisbu.api.budget.application.port.in.UpdateBudgetUseCase;
import com.fisbu.api.dto.BudgetRequest;
import com.fisbu.api.dto.BudgetResponse;
import com.fisbu.api.dto.BudgetSuggestionResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/budgets")
public class BudgetController {

    private final GetBudgetsUseCase getBudgetsUseCase;
    private final CreateBudgetUseCase createBudgetUseCase;
    private final UpdateBudgetUseCase updateBudgetUseCase;
    private final DeleteBudgetUseCase deleteBudgetUseCase;
    private final GetBudgetSuggestionUseCase getBudgetSuggestionUseCase;

    public BudgetController(GetBudgetsUseCase getBudgetsUseCase, CreateBudgetUseCase createBudgetUseCase,
                             UpdateBudgetUseCase updateBudgetUseCase, DeleteBudgetUseCase deleteBudgetUseCase,
                             GetBudgetSuggestionUseCase getBudgetSuggestionUseCase) {
        this.getBudgetsUseCase = getBudgetsUseCase;
        this.createBudgetUseCase = createBudgetUseCase;
        this.updateBudgetUseCase = updateBudgetUseCase;
        this.deleteBudgetUseCase = deleteBudgetUseCase;
        this.getBudgetSuggestionUseCase = getBudgetSuggestionUseCase;
    }

    @GetMapping
    public List<BudgetResponse> getBudgets(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestParam(required = false) Integer year,
                                            @RequestParam(required = false) Integer month) {
        return getBudgetsUseCase.getBudgets(userDetails.getUsername(), year, month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse createBudget(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody @Valid BudgetRequest request) {
        return createBudgetUseCase.createBudget(userDetails.getUsername(), request);
    }

    @GetMapping("/suggestion")
    public BudgetSuggestionResponse getBudgetSuggestion(@AuthenticationPrincipal UserDetails userDetails,
                                                         @RequestParam Long categoryId,
                                                         @RequestParam(required = false) Integer year,
                                                         @RequestParam(required = false) Integer month) {
        return getBudgetSuggestionUseCase.getBudgetSuggestion(userDetails.getUsername(), categoryId, year, month);
    }

    @PutMapping("/{id}")
    public BudgetResponse updateBudget(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable Long id,
                                        @RequestBody @Valid BudgetRequest request) {
        return updateBudgetUseCase.updateBudget(userDetails.getUsername(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        deleteBudgetUseCase.deleteBudget(userDetails.getUsername(), id);
    }
}
