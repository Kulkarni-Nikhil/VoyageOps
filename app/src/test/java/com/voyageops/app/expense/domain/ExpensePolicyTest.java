package com.voyageops.app.expense.domain;

import com.voyageops.app.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpensePolicyTest {

    private final ExpensePolicy policy = new ExpensePolicy();

    @Test
    void validateAmount_acceptsWholeAmounts() {
        assertThatCode(() -> policy.validateAmount(new BigDecimal("100")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAmount_acceptsUpToTwoDecimals() {
        assertThatCode(() -> policy.validateAmount(new BigDecimal("0.01")))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validateAmount(new BigDecimal("99.99")))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validateAmount(new BigDecimal("12.50")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAmount_rejectsNull() {
        assertThatThrownBy(() -> policy.validateAmount(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Amount is required");
    }

    @Test
    void validateAmount_rejectsZero() {
        assertThatThrownBy(() -> policy.validateAmount(BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Amount must be positive");
    }

    @Test
    void validateAmount_rejectsNegative() {
        assertThatThrownBy(() -> policy.validateAmount(new BigDecimal("-10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Amount must be positive");
    }

    @Test
    void validateAmount_rejectsMoreThanTwoDecimals() {
        assertThatThrownBy(() -> policy.validateAmount(new BigDecimal("12.345")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("more than 2 decimal places");
        assertThatThrownBy(() -> policy.validateAmount(new BigDecimal("0.001")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("more than 2 decimal places");
    }
}
