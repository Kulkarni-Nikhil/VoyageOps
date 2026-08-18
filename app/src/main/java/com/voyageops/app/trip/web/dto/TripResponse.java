package com.voyageops.app.trip.web.dto;

import com.voyageops.app.trip.domain.TripStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal budget;
    private LocalDate startDate;
    private LocalDate endDate;
    private TripStatus status;
    private UUID ownerId;
    private Long version;
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;
}