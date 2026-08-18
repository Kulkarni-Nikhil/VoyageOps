package com.voyageops.app.weather.domain;

import java.time.LocalDate;

public record DailyWeather(
        LocalDate date,
        Double tempMinC,
        Double tempMaxC,
        Integer precipitationProbability,
        int weatherCode) {
}
