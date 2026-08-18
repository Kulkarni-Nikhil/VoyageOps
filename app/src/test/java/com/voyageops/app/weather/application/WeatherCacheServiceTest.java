package com.voyageops.app.weather.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.trip.application.TripDestinationResolver;
import com.voyageops.app.trip.domain.Destination;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.weather.domain.DailyWeather;
import com.voyageops.app.weather.domain.GeoCoordinate;
import com.voyageops.app.weather.domain.Geocoder;
import com.voyageops.app.weather.domain.WeatherForecast;
import com.voyageops.app.weather.domain.WeatherProvider;
import com.voyageops.app.weather.web.dto.TripWeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherCacheServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDestinationResolver destinationResolver;

    @Mock
    private Geocoder geocoder;

    @Mock
    private WeatherProvider weatherProvider;

    private WeatherCacheService weatherCacheService;

    @BeforeEach
    void setUp() {
        weatherCacheService = new WeatherCacheService(tripRepository, destinationResolver, geocoder, weatherProvider);
    }

    @Test
    void getTripWeather_returnsForecastMappedToResponse() {
        Trip trip = Trip.builder()
                .id(1L)
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 3))
                .build();
        Destination destination = Destination.builder().city("Tokyo").country("Japan").build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(destinationResolver.primary(1L)).thenReturn(destination);
        when(destinationResolver.label(destination)).thenReturn("Tokyo, Japan");
        when(geocoder.geocode("Tokyo, Japan"))
                .thenReturn(Optional.of(new GeoCoordinate(35.68, 139.69)));
        when(weatherProvider.getForecast(any(GeoCoordinate.class), any(String.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new WeatherForecast("Tokyo, Japan", List.of(
                        new DailyWeather(LocalDate.of(2026, 9, 1), 21.0, 30.0, 10, 1))));

        TripWeatherResponse response = weatherCacheService.getTripWeather(1L);

        assertThat(response.getTripId()).isEqualTo(1L);
        assertThat(response.getDestination()).isEqualTo("Tokyo, Japan");
        assertThat(response.getLatitude()).isEqualTo(35.68);
        assertThat(response.getDays()).hasSize(1);
        assertThat(response.getDays().get(0).getWeatherCode()).isEqualTo(1);
        assertThat(response.getDays().get(0).getWeatherDescription()).isEqualTo("Mainly clear");
    }

    @Test
    void getTripWeather_rejectsTripWithoutDates() {
        Trip trip = Trip.builder().id(1L).build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> weatherCacheService.getTripWeather(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no date range");
    }

    @Test
    void getTripWeather_returnsEmptyDaysWhenTripStartsBeyondForecastHorizon() {
        Trip trip = Trip.builder()
                .id(1L)
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(40))
                .build();
        Destination destination = Destination.builder().city("Tokyo").country("Japan").build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(destinationResolver.primary(1L)).thenReturn(destination);
        when(destinationResolver.label(destination)).thenReturn("Tokyo, Japan");
        when(geocoder.geocode("Tokyo, Japan"))
                .thenReturn(Optional.of(new GeoCoordinate(35.68, 139.69)));

        TripWeatherResponse response = weatherCacheService.getTripWeather(1L);

        assertThat(response.getDays()).isEmpty();
        assertThat(response.getNotice()).contains("not yet available");
    }

    @Test
    void getTripWeather_failsWhenLocationCannotBeResolved() {
        Trip trip = Trip.builder()
                .id(1L)
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 3))
                .build();
        Destination destination = Destination.builder().city("Nowhere").build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(destinationResolver.primary(1L)).thenReturn(destination);
        when(destinationResolver.label(destination)).thenReturn("Nowhere");
        when(geocoder.geocode("Nowhere")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weatherCacheService.getTripWeather(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Could not resolve location");
    }
}
