package com.voyageops.app.places.infrastructure;

import com.voyageops.app.common.exception.ServiceUnavailableException;
import com.voyageops.app.places.domain.Place;
import com.voyageops.app.places.domain.PlacesProvider;
import com.voyageops.app.weather.domain.GeoCoordinate;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

/**
 * Discovers nearby points of interest via the OpenTripMap radius API.
 * Requires an API key (see {@code INTEGRATION_OPENTRIPMAP_API_KEY}); without
 * one the feature reports 503 Service Unavailable.
 */
@Component
public class OpenTripMapPlacesAdapter implements PlacesProvider {

    private static final String BASE_URL = "https://api.opentripmap.com";

    private final RestClient.Builder restClientBuilder;
    private final String apiKey;
    private RestClient restClient;

    public OpenTripMapPlacesAdapter(RestClient.Builder restClientBuilder,
                                    @Value("${integration.opentripmap.api-key:}") String apiKey) {
        this.restClientBuilder = restClientBuilder;
        this.apiKey = apiKey;
    }

    @PostConstruct
    void init() {
        restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    @Retryable(maxRetries = 2, delay = 500,
            excludes = {HttpClientErrorException.class, ServiceUnavailableException.class})
    @ConcurrencyLimit(4)
    @Override
    public List<Place> findNearby(GeoCoordinate coordinate, int radiusMeters, int limit) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ServiceUnavailableException("OpenTripMap API key is not configured");
        }
        RadiusResult[] results = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/0.1/en/places/radius")
                        .queryParam("radius", radiusMeters)
                        .queryParam("lon", coordinate.longitude())
                        .queryParam("lat", coordinate.latitude())
                        .queryParam("kinds", "interesting_places")
                        .queryParam("limit", limit)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(RadiusResult[].class);
        if (results == null) {
            return List.of();
        }
        return Arrays.stream(results)
                .map(result -> new Place(
                        result.xid(),
                        result.name(),
                        result.kinds(),
                        result.dist() != null ? result.dist() : 0,
                        result.rate()))
                .toList();
    }

    private record RadiusResult(String xid, String name, String kinds, Double dist, Double rate) {
    }
}
