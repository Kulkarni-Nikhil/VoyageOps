package com.voyageops.app.trip.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationRequest {

    @NotBlank(message = "City is required")
    private String city;

    private String country;

    @NotNull(message = "Arrival date is required")
    private LocalDate arrival;

    @NotNull(message = "Departure date is required")
    private LocalDate departure;
}