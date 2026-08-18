package com.voyageops.app.expense.web.dto;

import com.voyageops.app.expense.domain.ExpenseCategory;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExpenseRequest {

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private ExpenseCategory category;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private LocalDate spentOn;
}
