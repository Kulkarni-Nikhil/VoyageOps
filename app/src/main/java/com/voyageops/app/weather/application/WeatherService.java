package com.voyageops.app.weather.application;

import com.voyageops.app.user.application.TripMemberService;
import com.voyageops.app.weather.web.dto.TripWeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Authorized facade over the cached weather reader. Enforces trip membership
 * before any cached data is served.
 */
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final TripMemberService tripMemberService;
    private final WeatherCacheService weatherCacheService;

    public TripWeatherResponse getTripWeather(Long tripId, UUID actorId) {
        tripMemberService.requireMember(actorId, tripId);
        return weatherCacheService.getTripWeather(tripId);
    }
}
