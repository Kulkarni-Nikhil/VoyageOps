package com.voyageops.app.expense.application;

import com.voyageops.app.common.exception.AccessDeniedException;
import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.common.web.PageResponse;
import com.voyageops.app.expense.domain.Expense;
import com.voyageops.app.expense.domain.ExpenseCategory;
import com.voyageops.app.expense.domain.ExpensePolicy;
import com.voyageops.app.expense.infrastructure.ExpenseRepository;
import com.voyageops.app.expense.web.dto.BudgetSummaryResponse;
import com.voyageops.app.expense.web.dto.CreateExpenseRequest;
import com.voyageops.app.expense.web.dto.ExpenseResponse;
import com.voyageops.app.expense.web.dto.UpdateExpenseRequest;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.domain.TripStatus;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.user.application.TripMemberService;
import com.voyageops.app.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberService tripMemberService;

    private final ExpensePolicy expensePolicy = new ExpensePolicy();
    private ExpenseService expenseService;

    private final UUID ownerId = UUID.randomUUID();
    private final User owner = User.builder().id(ownerId).name("Alice").email("alice@example.com").build();
    private final Trip trip = Trip.builder()
            .id(1L)
            .name("Tokyo Trip")
            .owner(owner)
            .budget(new BigDecimal("1000.00"))
            .status(TripStatus.PLANNING)
            .build();

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(
                expenseRepository, tripRepository, expensePolicy, tripMemberService);
    }

    @Test
    void addExpense_ownerCanAdd() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .amount(new BigDecimal("45.50"))
                .category(ExpenseCategory.FOOD)
                .description("Ramen")
                .spentOn(LocalDate.of(2026, 9, 2))
                .build();

        when(tripMemberService.requireEditor(ownerId, 1L)).thenReturn(owner);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense expense = inv.getArgument(0);
            expense.setId(99L);
            return expense;
        });

        ExpenseResponse response = expenseService.addExpense(1L, ownerId, request);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getAmount()).isEqualByComparingTo("45.50");
        assertThat(response.getCategory()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(response.getPaidById()).isEqualTo(ownerId);
        assertThat(response.getPaidByName()).isEqualTo("Alice");
    }

    @Test
    void addExpense_nonEditorThrows() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .amount(new BigDecimal("45.50"))
                .build();

        when(tripMemberService.requireEditor(ownerId, 1L))
                .thenThrow(new AccessDeniedException("Only trip editors can perform this action"));

        assertThatThrownBy(() -> expenseService.addExpense(1L, ownerId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addExpense_rejectsNegativeAmount() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .amount(new BigDecimal("-10.00"))
                .build();

        when(tripMemberService.requireEditor(ownerId, 1L)).thenReturn(owner);

        assertThatThrownBy(() -> expenseService.addExpense(1L, ownerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Amount must be positive");
    }

    @Test
    void addExpense_rejectsTooManyDecimals() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .amount(new BigDecimal("12.345"))
                .build();

        when(tripMemberService.requireEditor(ownerId, 1L)).thenReturn(owner);

        assertThatThrownBy(() -> expenseService.addExpense(1L, ownerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("more than 2 decimal places");
    }

    @Test
    void getExpenses_returnsPage() {
        Expense expense = buildExpense();
        Pageable pageable = PageRequest.of(0, 20);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.findByTrip(trip, pageable))
                .thenReturn(new PageImpl<>(List.of(expense), pageable, 1));

        PageResponse<ExpenseResponse> page = expenseService.getExpenses(1L, ownerId, pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).getAmount()).isEqualByComparingTo("45.50");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void getExpenses_nonMemberThrows() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripMemberService.requireMember(ownerId, 1L))
                .thenThrow(new AccessDeniedException("You are not a member of this trip"));

        assertThatThrownBy(() -> expenseService.getExpenses(1L, ownerId, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getExpenseById_returnsExpense() {
        Expense expense = buildExpense();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.findByIdAndTrip(1L, trip)).thenReturn(Optional.of(expense));

        ExpenseResponse response = expenseService.getExpenseById(1L, 1L, ownerId);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPaidByName()).isEqualTo("Alice");
    }

    @Test
    void getExpenseById_notInTripThrows() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.findByIdAndTrip(99L, trip)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(1L, 99L, ownerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Expense not found");
    }

    @Test
    void updateExpense_editorUpdates() {
        Expense expense = buildExpense();
        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .amount(new BigDecimal("60.00"))
                .category(ExpenseCategory.TRANSPORT)
                .build();

        when(tripMemberService.requireEditor(ownerId, 1L)).thenReturn(owner);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.findByIdAndTrip(1L, trip)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseResponse response = expenseService.updateExpense(1L, 1L, ownerId, request);

        assertThat(response.getAmount()).isEqualByComparingTo("60.00");
        assertThat(response.getCategory()).isEqualTo(ExpenseCategory.TRANSPORT);
    }

    @Test
    void updateExpense_memberThrows() {
        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .amount(new BigDecimal("60.00"))
                .build();

        when(tripMemberService.requireEditor(ownerId, 1L))
                .thenThrow(new AccessDeniedException("Only trip editors can perform this action"));

        assertThatThrownBy(() -> expenseService.updateExpense(1L, 1L, ownerId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateExpense_rejectsInvalidAmount() {
        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .amount(new BigDecimal("-1.00"))
                .build();

        when(tripMemberService.requireEditor(ownerId, 1L)).thenReturn(owner);

        assertThatThrownBy(() -> expenseService.updateExpense(1L, 1L, ownerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Amount must be positive");
    }

    @Test
    void deleteExpense_editorDeletes() {
        Expense expense = buildExpense();
        when(tripMemberService.requireEditor(ownerId, 1L)).thenReturn(owner);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.findByIdAndTrip(1L, trip)).thenReturn(Optional.of(expense));

        expenseService.deleteExpense(1L, 1L, ownerId);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void getBudgetSummary_computesTotalsAndCategories() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.sumAmountByTrip(trip)).thenReturn(new BigDecimal("600.00"));
        when(expenseRepository.groupSumByCategory(trip)).thenReturn(List.of(
                new TestCategoryTotal(ExpenseCategory.FOOD, new BigDecimal("400.00")),
                new TestCategoryTotal(ExpenseCategory.TRANSPORT, new BigDecimal("200.00"))));
        when(expenseRepository.countByTrip(trip)).thenReturn(2L);

        BudgetSummaryResponse summary = expenseService.getBudgetSummary(1L, ownerId);

        assertThat(summary.getBudget()).isEqualByComparingTo("1000.00");
        assertThat(summary.getTotalSpent()).isEqualByComparingTo("600.00");
        assertThat(summary.getRemaining()).isEqualByComparingTo("400.00");
        assertThat(summary.getSpentPercentage()).isEqualByComparingTo("60.00");
        assertThat(summary.getExpenseCount()).isEqualTo(2);
        assertThat(summary.getByCategory())
                .containsEntry(ExpenseCategory.FOOD, new BigDecimal("400.00"))
                .containsEntry(ExpenseCategory.TRANSPORT, new BigDecimal("200.00"));
    }

    @Test
    void getBudgetSummary_nullBudgetHasNoRemainingOrPercentage() {
        Trip noBudgetTrip = Trip.builder()
                .id(1L)
                .name("Tokyo Trip")
                .owner(owner)
                .status(TripStatus.PLANNING)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(noBudgetTrip));
        when(expenseRepository.sumAmountByTrip(noBudgetTrip)).thenReturn(new BigDecimal("50.00"));
        when(expenseRepository.groupSumByCategory(noBudgetTrip)).thenReturn(List.of());
        when(expenseRepository.countByTrip(noBudgetTrip)).thenReturn(1L);

        BudgetSummaryResponse summary = expenseService.getBudgetSummary(1L, ownerId);

        assertThat(summary.getBudget()).isNull();
        assertThat(summary.getRemaining()).isNull();
        assertThat(summary.getSpentPercentage()).isNull();
        assertThat(summary.getTotalSpent()).isEqualByComparingTo("50.00");
        assertThat(summary.getByCategory()).isEmpty();
    }

    private Expense buildExpense() {
        return Expense.builder()
                .id(1L)
                .trip(trip)
                .paidBy(owner)
                .amount(new BigDecimal("45.50"))
                .category(ExpenseCategory.FOOD)
                .description("Ramen")
                .spentOn(LocalDate.of(2026, 9, 2))
                .createdAt(Instant.now())
                .build();
    }

    private record TestCategoryTotal(ExpenseCategory category, BigDecimal total)
            implements ExpenseRepository.CategoryTotal {
        @Override
        public ExpenseCategory getCategory() {
            return category;
        }

        @Override
        public BigDecimal getTotal() {
            return total;
        }
    }
}
