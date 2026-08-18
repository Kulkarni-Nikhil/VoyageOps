package com.voyageops.app.trip.web.dto;

import com.voyageops.app.trip.domain.ActivityStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    private Long id;
    private Long tripId;
    private String title;
    private String description;
    private BigDecimal cost;
    private ActivityStatus status;
    private LocalDate scheduledFor;
}