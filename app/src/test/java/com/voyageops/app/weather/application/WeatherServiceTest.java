package com.voyageops.app.weather.application;

import com.voyageops.app.user.application.TripMemberService;
import com.voyageops.app.weather.web.dto.TripWeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private TripMemberService tripMemberService;

    @Mock
    private WeatherCacheService weatherCacheService;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(tripMemberService, weatherCacheService);
    }

    @Test
    void getTripWeather_enforcesMembershipThenDelegates() {
        UUID actorId = UUID.randomUUID();
        TripWeatherResponse response = TripWeatherResponse.builder().tripId(1L).build();
        when(weatherCacheService.getTripWeather(1L)).thenReturn(response);

        TripWeatherResponse result = weatherService.getTripWeather(1L, actorId);

        verify(tripMemberService).requireMember(actorId, 1L);
        assertThat(result.getTripId()).isEqualTo(1L);
    }
}
