package com.voyageops.app.expense.application;

import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.common.web.PageResponse;
import com.voyageops.app.config.CacheConfig;
import com.voyageops.app.expense.domain.Expense;
import com.voyageops.app.expense.domain.ExpenseCategory;
import com.voyageops.app.expense.domain.ExpensePolicy;
import com.voyageops.app.expense.infrastructure.ExpenseRepository;
import com.voyageops.app.expense.web.dto.BudgetSummaryResponse;
import com.voyageops.app.expense.web.dto.CreateExpenseRequest;
import com.voyageops.app.expense.web.dto.ExpenseResponse;
import com.voyageops.app.expense.web.dto.UpdateExpenseRequest;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.user.application.TripMemberService;
import com.voyageops.app.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final ExpensePolicy expensePolicy;
    private final TripMemberService tripMemberService;

    @CacheEvict(cacheNames = CacheConfig.CACHE_EXPENSE_SUMMARY, key = "#tripId")
    @Transactional
    public ExpenseResponse addExpense(Long tripId, UUID actorId, CreateExpenseRequest request) {
        User paidBy = tripMemberService.requireEditor(actorId, tripId);
        expensePolicy.validateAmount(request.getAmount());
        Trip trip = getTrip(tripId);

        Expense expense = Expense.builder()
                .trip(trip)
                .paidBy(paidBy)
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .spentOn(request.getSpentOn())
                .build();
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> getExpenses(Long tripId, UUID actorId, Pageable pageable) {
        Trip trip = getTrip(tripId);
        tripMemberService.requireMember(actorId, tripId);
        Page<ExpenseResponse> page = expenseRepository.findByTrip(trip, pageable)
                .map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long tripId, Long expenseId, UUID actorId) {
        tripMemberService.requireMember(actorId, tripId);
        return toResponse(getExpense(tripId, expenseId));
    }

    @CacheEvict(cacheNames = CacheConfig.CACHE_EXPENSE_SUMMARY, key = "#tripId")
    @Transactional
    public ExpenseResponse updateExpense(Long tripId, Long expenseId, UUID actorId,
                                         UpdateExpenseRequest request) {
        tripMemberService.requireEditor(actorId, tripId);
        if (request.getAmount() != null) {
            expensePolicy.validateAmount(request.getAmount());
        }
        Expense expense = getExpense(tripId, expenseId);
        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }
        if (request.getCategory() != null) {
            expense.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getSpentOn() != null) {
            expense.setSpentOn(request.getSpentOn());
        }
        return toResponse(expenseRepository.save(expense));
    }

    @CacheEvict(cacheNames = CacheConfig.CACHE_EXPENSE_SUMMARY, key = "#tripId")
    @Transactional
    public void deleteExpense(Long tripId, Long expenseId, UUID actorId) {
        tripMemberService.requireEditor(actorId, tripId);
        Expense expense = getExpense(tripId, expenseId);
        expenseRepository.delete(expense);
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_EXPENSE_SUMMARY, key = "#tripId")
    @Transactional(readOnly = true)
    public BudgetSummaryResponse getBudgetSummary(Long tripId, UUID actorId) {
        Trip trip = getTrip(tripId);
        tripMemberService.requireMember(actorId, tripId);

        BigDecimal totalSpent = expenseRepository.sumAmountByTrip(trip);
        BigDecimal budget = trip.getBudget();
        BigDecimal remaining = budget != null ? budget.subtract(totalSpent) : null;
        BigDecimal spentPercentage = null;
        if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
            spentPercentage = totalSpent.multiply(BigDecimal.valueOf(100))
                    .divide(budget, 2, RoundingMode.HALF_UP);
        }

        Map<ExpenseCategory, BigDecimal> byCategory = new EnumMap<>(ExpenseCategory.class);
        expenseRepository.groupSumByCategory(trip)
                .forEach(total -> byCategory.put(total.getCategory(), total.getTotal()));

        return BudgetSummaryResponse.builder()
                .tripId(trip.getId())
                .tripName(trip.getName())
                .budget(budget)
                .totalSpent(totalSpent)
                .remaining(remaining)
                .spentPercentage(spentPercentage)
                .expenseCount(expenseRepository.countByTrip(trip))
                .byCategory(byCategory)
                .build();
    }

    private Expense getExpense(Long tripId, Long expenseId) {
        Trip trip = getTrip(tripId);
        return expenseRepository.findByIdAndTrip(expenseId, trip)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
    }

    private Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
    }

    private ExpenseResponse toResponse(Expense expense) {
        User paidBy = expense.getPaidBy();
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .spentOn(expense.getSpentOn())
                .paidById(paidBy != null ? paidBy.getId() : null)
                .paidByName(paidBy != null ? paidBy.getName() : null)
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
