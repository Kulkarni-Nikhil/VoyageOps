package com.voyageops.app.weather.infrastructure;

import com.voyageops.app.weather.domain.DailyWeather;
import com.voyageops.app.weather.domain.GeoCoordinate;
import com.voyageops.app.weather.domain.WeatherForecast;
import com.voyageops.app.weather.domain.WeatherProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches daily forecasts from the Open-Meteo weather API (keyless).
 */
@Component
@RequiredArgsConstructor
public class OpenMeteoWeatherAdapter implements WeatherProvider {

    private static final String BASE_URL = "https://api.open-meteo.com";

    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;

    @PostConstruct
    void init() {
        restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    @Retryable(maxRetries = 2, delay = 500, excludes = HttpClientErrorException.class)
    @ConcurrencyLimit(4)
    @Override
    public WeatherForecast getForecast(GeoCoordinate coordinate, String location, LocalDate from, LocalDate to) {
        ForecastResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v1/forecast")
                        .queryParam("latitude", coordinate.latitude())
                        .queryParam("longitude", coordinate.longitude())
                        .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode")
                        .queryParam("timezone", "auto")
                        .queryParam("start_date", from.toString())
                        .queryParam("end_date", to.toString())
                        .build())
                .retrieve()
                .body(ForecastResponse.class);

        return new WeatherForecast(location, toDays(response));
    }

    private List<DailyWeather> toDays(ForecastResponse response) {
        List<DailyWeather> days = new ArrayList<>();
        if (response == null || response.daily() == null) {
            return days;
        }
        Daily daily = response.daily();
        List<String> times = daily.time() == null ? List.of() : daily.time();
        for (int i = 0; i < times.size(); i++) {
            days.add(new DailyWeather(
                    LocalDate.parse(times.get(i)),
                    valueAt(daily.temperature_2m_min(), i),
                    valueAt(daily.temperature_2m_max(), i),
                    intAt(daily.precipitation_probability_max(), i),
                    intAt(daily.weathercode(), i)));
        }
        return days;
    }

    private static Double valueAt(List<Double> values, int index) {
        return values != null && index < values.size() ? values.get(index) : null;
    }

    private static int intAt(List<Integer> values, int index) {
        return values != null && index < values.size() && values.get(index) != null ? values.get(index) : 0;
    }

    private record ForecastResponse(Daily daily) {
    }

    private record Daily(List<String> time,
                         List<Double> temperature_2m_max,
                         List<Double> temperature_2m_min,
                         List<Integer> precipitation_probability_max,
                         List<Integer> weathercode) {
    }
}
