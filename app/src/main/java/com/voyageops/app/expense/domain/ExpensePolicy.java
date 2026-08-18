package com.voyageops.app.expense.domain;

import com.voyageops.app.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Money rules for expense amounts.
 * <p>
 * Enforces the database {@code CHECK (amount > 0)} constraint plus the
 * {@code NUMERIC(12,2)} scale at the domain layer, so precision rules are
 * never silently lost at the boundary.
 */
@Component
public class ExpensePolicy {

    private static final int MAX_SCALE = 2;

    public void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be positive");
        }
        if (amount.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new BusinessException("Amount cannot have more than 2 decimal places");
        }
    }
}
