package com.voyageops.app.trip.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRequest {

    @NotBlank(message = "Activity title is required")
    private String title;

    private String description;

    @Positive(message = "Activity cost must be positive")
    private BigDecimal cost;

    private LocalDate scheduledFor;
}