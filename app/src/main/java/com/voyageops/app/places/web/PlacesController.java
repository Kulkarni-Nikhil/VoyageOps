package com.voyageops.app.places.web;

import com.voyageops.app.common.web.CurrentUserProvider;
import com.voyageops.app.places.application.PlacesService;
import com.voyageops.app.places.web.dto.TripPlacesResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips/{tripId}/places")
@Tag(name = "Places")
public class PlacesController {

    private final PlacesService placesService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public TripPlacesResponse getTripPlaces(@PathVariable Long tripId,
                                            @RequestParam(defaultValue = "5000") int radiusMeters) {
        return placesService.getTripPlaces(tripId, actor(), radiusMeters);
    }

    private UUID actor() {
        return currentUserProvider.currentUserId();
    }
}
