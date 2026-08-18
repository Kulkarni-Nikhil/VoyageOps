package com.voyageops.app.weather.web.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripWeatherResponse {
    private Long tripId;
    private String destination;
    private Double latitude;
    private Double longitude;
    private Instant generatedAt;
    private String notice;
    private List<WeatherDayResponse> days;
}
