package com.voyageops.app.weather.infrastructure;

import com.voyageops.app.weather.domain.GeoCoordinate;
import com.voyageops.app.weather.domain.Geocoder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * Resolves locations via the Open-Meteo geocoding API (keyless).
 */
@Component
@RequiredArgsConstructor
public class OpenMeteoGeocodingAdapter implements Geocoder {

    private static final String BASE_URL = "https://geocoding-api.open-meteo.com";

    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;

    @PostConstruct
    void init() {
        restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    @Retryable(maxRetries = 2, delay = 500, excludes = HttpClientErrorException.class)
    @ConcurrencyLimit(4)
    @Override
    public Optional<GeoCoordinate> geocode(String query) {
        GeocodingResponse response = restClient.get()
                .uri("/v1/search?name={name}&count=1", query)
                .retrieve()
                .body(GeocodingResponse.class);
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return Optional.empty();
        }
        Result result = response.results().get(0);
        return Optional.of(new GeoCoordinate(result.latitude(), result.longitude()));
    }

    private record GeocodingResponse(List<Result> results) {
    }

    private record Result(double latitude, double longitude) {
    }
}
