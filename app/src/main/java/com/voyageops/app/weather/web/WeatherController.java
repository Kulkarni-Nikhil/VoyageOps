package com.voyageops.app.weather.web;

import com.voyageops.app.common.web.CurrentUserProvider;
import com.voyageops.app.weather.application.WeatherService;
import com.voyageops.app.weather.web.dto.TripWeatherResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips/{tripId}/weather")
@Tag(name = "Weather")
public class WeatherController {

    private final WeatherService weatherService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public TripWeatherResponse getTripWeather(@PathVariable Long tripId) {
        return weatherService.getTripWeather(tripId, actor());
    }

    private UUID actor() {
        return currentUserProvider.currentUserId();
    }
}
