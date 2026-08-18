package com.voyageops.app.trip.web.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationResponse {
    private Long id;
    private String city;
    private String country;
    private LocalDate arrival;
    private LocalDate departure;
}