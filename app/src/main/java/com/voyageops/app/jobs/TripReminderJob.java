package com.voyageops.app.jobs;

import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Emits reminders for trips starting tomorrow.
 * <p>
 * Notification delivery (email/push) is out of scope for now; reminders are
 * logged as structured events. The seam is in place for a later notifier.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripReminderJob {

    private final TripRepository tripRepository;

    @Scheduled(cron = "0 0 8 * * *")
    public void remindUpcomingTrips() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Trip> trips = tripRepository.findByStartDate(tomorrow);
        if (trips.isEmpty()) {
            log.info("No trips starting tomorrow ({})", tomorrow);
            return;
        }
        trips.forEach(trip -> log.info(
                "Trip reminder: '{}' (id={}) starts tomorrow ({})",
                trip.getName(), trip.getId(), trip.getStartDate()));
    }
}