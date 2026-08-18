package com.voyageops.app.weather.domain;

import java.util.List;

public record WeatherForecast(String location, List<DailyWeather> days) {
}
