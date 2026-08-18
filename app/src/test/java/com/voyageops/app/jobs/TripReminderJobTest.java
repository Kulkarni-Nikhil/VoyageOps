package com.voyageops.app.jobs;

import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.domain.TripStatus;
import com.voyageops.app.trip.infrastructure.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripReminderJobTest {

    @Mock
    private TripRepository tripRepository;

    private TripReminderJob job;

    @BeforeEach
    void setUp() {
        job = new TripReminderJob(tripRepository);
    }

    @Test
    void remindUpcomingTrips_queriesTripsStartingTomorrow() {
        Trip trip = Trip.builder()
                .id(1L)
                .name("Tokyo Trip")
                .startDate(LocalDate.now().plusDays(1))
                .status(TripStatus.PLANNING)
                .build();
        when(tripRepository.findByStartDate(any(LocalDate.class))).thenReturn(List.of(trip));

        job.remindUpcomingTrips();

        verify(tripRepository).findByStartDate(any(LocalDate.class));
    }

    @Test
    void remindUpcomingTrips_handlesEmptyResult() {
        when(tripRepository.findByStartDate(any(LocalDate.class))).thenReturn(List.of());

        job.remindUpcomingTrips();

        verify(tripRepository).findByStartDate(any(LocalDate.class));
    }
}
