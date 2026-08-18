package com.voyageops.app.trip.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.config.CacheConfig;
import com.voyageops.app.trip.domain.Destination;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.DestinationRepository;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.trip.web.dto.DestinationRequest;
import com.voyageops.app.trip.web.dto.DestinationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final TripRepository tripRepository;

    @Cacheable(cacheNames = CacheConfig.CACHE_DESTINATIONS, key = "#tripId")
    @Transactional(readOnly = true)
    public List<DestinationResponse> getDestinationByTrip(Long tripId) {
        return destinationRepository.findByTripId(tripId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DestinationResponse getDestinationById(Long id) {
        return toResponse(getDestination(id));
    }

    @CacheEvict(cacheNames = {CacheConfig.CACHE_DESTINATIONS, CacheConfig.CACHE_WEATHER, CacheConfig.CACHE_PLACES}, key = "#tripId")
    @Transactional
    public DestinationResponse addDestination(Long tripId, DestinationRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

        validateDestinationDates(trip, request);

        Destination destination = Destination.builder()
                .city(request.getCity())
                .country(request.getCountry())
                .arrival(request.getArrival())
                .departure(request.getDeparture())
                .trip(trip)
                .build();

        return toResponse(destinationRepository.save(destination));
    }

    @CacheEvict(cacheNames = {CacheConfig.CACHE_DESTINATIONS, CacheConfig.CACHE_WEATHER, CacheConfig.CACHE_PLACES}, key = "#result.tripId")
    @Transactional
    public DestinationResponse updateDestination(Long id, DestinationRequest request) {
        Destination destination = getDestination(id);

        destination.setCity(request.getCity());
        destination.setCountry(request.getCountry());
        destination.setArrival(request.getArrival());
        destination.setDeparture(request.getDeparture());

        return toResponse(destinationRepository.save(destination));
    }

    @CacheEvict(cacheNames = {CacheConfig.CACHE_DESTINATIONS, CacheConfig.CACHE_WEATHER, CacheConfig.CACHE_PLACES}, key = "#result.tripId")
    @Transactional
    public DestinationResponse deleteDestination(Long id) {
        Destination destination = getDestination(id);
        destinationRepository.delete(destination);
        return toResponse(destination);
    }

    private Destination getDestination(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination", id));
    }

    private void validateDestinationDates(Trip trip, DestinationRequest request) {
        if (request.getArrival().isAfter(request.getDeparture())) {
            throw new BusinessException("Arrival date cannot be after departure date");
        }
        if (request.getArrival().isBefore(trip.getStartDate())) {
            throw new BusinessException("Arrival must be within trip duration");
        }
        if (request.getDeparture().isAfter(trip.getEndDate())) {
            throw new BusinessException("Departure must be within trip duration");
        }
    }

    private DestinationResponse toResponse(Destination destination) {
        return DestinationResponse.builder()
                .id(destination.getId())
                .tripId(destination.getTrip() != null ? destination.getTrip().getId() : null)
                .city(destination.getCity())
                .country(destination.getCountry())
                .arrival(destination.getArrival())
                .departure(destination.getDeparture())
                .build();
    }
}