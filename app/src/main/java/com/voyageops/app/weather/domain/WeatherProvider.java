package com.voyageops.app.weather.domain;

import java.time.LocalDate;

/**
 * Fetches daily weather for a location over a date range.
 */
public interface WeatherProvider {

    WeatherForecast getForecast(GeoCoordinate coordinate, String location, LocalDate from, LocalDate to);
}
