package com.voyageops.app.expense.web;

import com.voyageops.app.common.web.CurrentUserProvider;
import com.voyageops.app.common.web.PageResponse;
import com.voyageops.app.expense.application.ExpenseService;
import com.voyageops.app.expense.web.dto.BudgetSummaryResponse;
import com.voyageops.app.expense.web.dto.CreateExpenseRequest;
import com.voyageops.app.expense.web.dto.ExpenseResponse;
import com.voyageops.app.expense.web.dto.UpdateExpenseRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips/{tripId}/expenses")
@Tag(name = "Expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse addExpense(@PathVariable Long tripId,
                                      @Valid @RequestBody CreateExpenseRequest request) {
        return expenseService.addExpense(tripId, actor(), request);
    }

    @GetMapping
    public PageResponse<ExpenseResponse> getExpenses(
            @PathVariable Long tripId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return expenseService.getExpenses(tripId, actor(), pageable);
    }

    @GetMapping("/summary")
    public BudgetSummaryResponse getSummary(@PathVariable Long tripId) {
        return expenseService.getBudgetSummary(tripId, actor());
    }

    @GetMapping("/{expenseId}")
    public ExpenseResponse getExpense(@PathVariable Long tripId, @PathVariable Long expenseId) {
        return expenseService.getExpenseById(tripId, expenseId, actor());
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponse updateExpense(@PathVariable Long tripId,
                                         @PathVariable Long expenseId,
                                         @Valid @RequestBody UpdateExpenseRequest request) {
        return expenseService.updateExpense(tripId, expenseId, actor(), request);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long tripId, @PathVariable Long expenseId) {
        expenseService.deleteExpense(tripId, expenseId, actor());
    }

    private UUID actor() {
        return currentUserProvider.currentUserId();
    }
}
