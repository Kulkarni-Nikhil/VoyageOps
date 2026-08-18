package com.voyageops.app.expense.web.dto;

import com.voyageops.app.expense.domain.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private BigDecimal amount;
    private ExpenseCategory category;
    private String description;
    private LocalDate spentOn;
    private UUID paidById;
    private String paidByName;
    private Instant createdAt;
}
