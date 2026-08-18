package com.voyageops.app.expense.web.dto;

import com.voyageops.app.expense.domain.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSummaryResponse {
    private Long tripId;
    private String tripName;
    private BigDecimal budget;
    private BigDecimal totalSpent;
    private BigDecimal remaining;
    private BigDecimal spentPercentage;
    private long expenseCount;
    private Map<ExpenseCategory, BigDecimal> byCategory;
}
