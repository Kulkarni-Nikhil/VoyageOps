package com.voyageops.app.jobs;

import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.weather.application.WeatherCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Prewarms the weather cache for trips starting within the next seven days.
 * <p>
 * Runs hourly so forecast data stays reasonably fresh while the first user
 * request of the day typically hits a warm cache. Failing trips are logged and
 * skipped rather than aborting the run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherCacheRefreshJob {

    private final TripRepository tripRepository;
    private final WeatherCacheService weatherCacheService;

    @Scheduled(cron = "0 0 * * * *")
    public void prewarmWeatherCache() {
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(7);
        List<Trip> trips = tripRepository.findByStartDateBetween(today, horizon);

        int refreshed = 0;
        for (Trip trip : trips) {
            try {
                weatherCacheService.getTripWeather(trip.getId());
                refreshed++;
            } catch (Exception e) {
                log.warn("Weather prewarm failed for trip {}: {}", trip.getId(), e.getMessage());
            }
        }
        log.info("Weather prewarm run completed: refreshed {} of {} upcoming trip(s)", refreshed, trips.size());
    }
}
