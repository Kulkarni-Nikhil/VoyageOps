package com.voyageops.app.jobs;

import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.domain.TripStatus;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.weather.application.WeatherCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherCacheRefreshJobTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private WeatherCacheService weatherCacheService;

    private WeatherCacheRefreshJob job;

    @BeforeEach
    void setUp() {
        job = new WeatherCacheRefreshJob(tripRepository, weatherCacheService);
    }

    @Test
    void prewarmWeatherCache_refreshesUpcomingTrips() {
        Trip trip = Trip.builder().id(1L).startDate(LocalDate.now().plusDays(1)).status(TripStatus.PLANNING).build();
        when(tripRepository.findByStartDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(trip));

        job.prewarmWeatherCache();

        verify(weatherCacheService).getTripWeather(1L);
    }

    @Test
    void prewarmWeatherCache_skipsFailingTrips() {
        Trip good = Trip.builder().id(1L).startDate(LocalDate.now().plusDays(1)).build();
        Trip bad = Trip.builder().id(2L).startDate(LocalDate.now().plusDays(2)).build();
        when(tripRepository.findByStartDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(bad, good));
        doThrow(new IllegalStateException("upstream down")).when(weatherCacheService).getTripWeather(2L);

        job.prewarmWeatherCache();

        verify(weatherCacheService).getTripWeather(1L);
        verify(weatherCacheService, never()).getTripWeather(3L);
    }
}
