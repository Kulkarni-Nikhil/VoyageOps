package com.voyageops.app.trip.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTripRequest {

    @NotBlank(message = "Trip name is required")
    private String name;

    private String description;

    @Positive(message = "Budget must be positive")
    private BigDecimal budget;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /**
     * Temporary stand-in for the authenticated actor until OAuth2 login lands.
     */
    private UUID ownerId;
}