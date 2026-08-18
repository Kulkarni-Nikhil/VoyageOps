package com.voyageops.app.places.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.config.CacheConfig;
import com.voyageops.app.places.domain.Place;
import com.voyageops.app.places.domain.PlacesProvider;
import com.voyageops.app.places.web.dto.PlaceResponse;
import com.voyageops.app.places.web.dto.TripPlacesResponse;
import com.voyageops.app.trip.application.TripDestinationResolver;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.weather.domain.GeoCoordinate;
import com.voyageops.app.weather.domain.Geocoder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cached places reader. Deliberately free of authorization logic; reachable
 * only through {@link PlacesService}, which enforces membership first.
 */
@Service
@RequiredArgsConstructor
public class PlacesCacheService {

    private static final int MAX_RADIUS = 50_000;

    private final TripRepository tripRepository;
    private final TripDestinationResolver destinationResolver;
    private final Geocoder geocoder;
    private final PlacesProvider placesProvider;

    @Cacheable(cacheNames = CacheConfig.CACHE_PLACES, key = "#tripId + '-' + #radiusMeters")
    @Transactional(readOnly = true)
    public TripPlacesResponse getTripPlaces(Long tripId, int radiusMeters) {
        tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

        int radius = Math.min(Math.max(radiusMeters, 100), MAX_RADIUS);
        String destinationLabel = destinationResolver.label(destinationResolver.primary(tripId));

        GeoCoordinate coordinate = geocoder.geocode(destinationLabel)
                .orElseThrow(() -> new BusinessException("Could not resolve location: " + destinationLabel));

        List<PlaceResponse> places = placesProvider.findNearby(coordinate, radius, 20).stream()
                .map(this::toPlace)
                .toList();

        return TripPlacesResponse.builder()
                .tripId(tripId)
                .destination(destinationLabel)
                .radiusMeters(radius)
                .latitude(coordinate.latitude())
                .longitude(coordinate.longitude())
                .places(places)
                .build();
    }

    private PlaceResponse toPlace(Place place) {
        return PlaceResponse.builder()
                .xid(place.xid())
                .name(place.name())
                .kinds(place.kinds())
                .distanceMeters(place.distanceMeters())
                .rate(place.rate())
                .build();
    }
}
