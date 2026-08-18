package com.voyageops.app.weather.web.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherDayResponse {
    private LocalDate date;
    private Double tempMinC;
    private Double tempMaxC;
    private Integer precipitationProbability;
    private Integer weatherCode;
    private String weatherDescription;
}
