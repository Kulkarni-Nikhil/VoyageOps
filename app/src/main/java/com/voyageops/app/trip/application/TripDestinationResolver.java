package com.voyageops.app.trip.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.trip.domain.Destination;
import com.voyageops.app.trip.infrastructure.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * Resolves the primary destination of a trip: the destination with the
 * earliest arrival, used by weather/places integrations.
 */
@Component
@RequiredArgsConstructor
public class TripDestinationResolver {

    private final DestinationRepository destinationRepository;

    public Destination primary(Long tripId) {
        return destinationRepository.findByTripId(tripId).stream()
                .min(Comparator.comparing(d -> d.getArrival() == null ? LocalDate.MAX : d.getArrival()))
                .orElseThrow(() -> new BusinessException("Trip has no destination for weather or places"));
    }

    public String label(Destination destination) {
        if (destination.getCountry() == null || destination.getCountry().isBlank()) {
            return destination.getCity();
        }
        return destination.getCity() + ", " + destination.getCountry();
    }
}
