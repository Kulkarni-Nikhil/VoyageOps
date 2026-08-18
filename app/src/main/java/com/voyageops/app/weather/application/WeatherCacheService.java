package com.voyageops.app.weather.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.config.CacheConfig;
import com.voyageops.app.trip.application.TripDestinationResolver;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.weather.domain.DailyWeather;
import com.voyageops.app.weather.domain.GeoCoordinate;
import com.voyageops.app.weather.domain.Geocoder;
import com.voyageops.app.weather.domain.WeatherForecast;
import com.voyageops.app.weather.domain.WeatherProvider;
import com.voyageops.app.weather.domain.WmoWeatherCode;
import com.voyageops.app.weather.web.dto.TripWeatherResponse;
import com.voyageops.app.weather.web.dto.WeatherDayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Cached weather reader. Deliberately free of authorization logic: it is the
 * prewarm target of the background job and is only reachable through
 * {@link WeatherService}, which enforces membership first.
 */
@Service
@RequiredArgsConstructor
public class WeatherCacheService {

    private static final int FORECAST_HORIZON_DAYS = 15;

    private final TripRepository tripRepository;
    private final TripDestinationResolver destinationResolver;
    private final Geocoder geocoder;
    private final WeatherProvider weatherProvider;

    @Cacheable(cacheNames = CacheConfig.CACHE_WEATHER, key = "#tripId")
    @Transactional(readOnly = true)
    public TripWeatherResponse getTripWeather(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
        if (trip.getStartDate() == null || trip.getEndDate() == null) {
            throw new BusinessException("Trip has no date range for a weather forecast");
        }
        String location = destinationResolver.label(destinationResolver.primary(tripId));

        GeoCoordinate coordinate = geocoder.geocode(location)
                .orElseThrow(() -> new BusinessException("Could not resolve location: " + location));

        LocalDate horizon = LocalDate.now().plusDays(FORECAST_HORIZON_DAYS);
        if (trip.getStartDate().isAfter(horizon)) {
            return TripWeatherResponse.builder()
                    .tripId(tripId)
                    .destination(location)
                    .latitude(coordinate.latitude())
                    .longitude(coordinate.longitude())
                    .generatedAt(Instant.now())
                    .days(List.of())
                    .notice("Forecast not yet available: trip starts " + trip.getStartDate())
                    .build();
        }

        LocalDate effectiveEnd = trip.getEndDate().isAfter(horizon) ? horizon : trip.getEndDate();
        WeatherForecast forecast = weatherProvider.getForecast(coordinate, location,
                trip.getStartDate(), effectiveEnd);

        List<WeatherDayResponse> days = forecast.days().stream()
                .map(this::toDay)
                .toList();

        return TripWeatherResponse.builder()
                .tripId(tripId)
                .destination(location)
                .latitude(coordinate.latitude())
                .longitude(coordinate.longitude())
                .generatedAt(Instant.now())
                .days(days)
                .notice(effectiveEnd.isBefore(trip.getEndDate())
                        ? "Forecast available only through " + effectiveEnd
                        : null)
                .build();
    }

    private WeatherDayResponse toDay(DailyWeather day) {
        return WeatherDayResponse.builder()
                .date(day.date())
                .tempMinC(day.tempMinC())
                .tempMaxC(day.tempMaxC())
                .precipitationProbability(day.precipitationProbability())
                .weatherCode(day.weatherCode())
                .weatherDescription(WmoWeatherCode.describe(day.weatherCode()))
                .build();
    }
}
