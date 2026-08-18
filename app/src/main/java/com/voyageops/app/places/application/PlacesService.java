package com.voyageops.app.places.application;

import com.voyageops.app.places.web.dto.TripPlacesResponse;
import com.voyageops.app.user.application.TripMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Authorized facade over the cached places reader. Enforces trip membership
 * before any cached data is served.
 */
@Service
@RequiredArgsConstructor
public class PlacesService {

    private final TripMemberService tripMemberService;
    private final PlacesCacheService placesCacheService;

    public TripPlacesResponse getTripPlaces(Long tripId, UUID actorId, int radiusMeters) {
        tripMemberService.requireMember(actorId, tripId);
        return placesCacheService.getTripPlaces(tripId, radiusMeters);
    }
}
