package com.voyageops.app.trip.web;

import com.voyageops.app.common.web.PageResponse;
import com.voyageops.app.trip.application.TripService;
import com.voyageops.app.trip.web.dto.CreateTripRequest;
import com.voyageops.app.trip.web.dto.TripResponse;
import com.voyageops.app.trip.web.dto.UpdateTripRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Trips", description = "Trip planning endpoints")
@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    @Operation(summary = "List trips (paginated)")
    @GetMapping
    public PageResponse<TripResponse> getAllTrips(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("Fetching trips, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return tripService.getAllTrips(pageable);
    }

    @Operation(summary = "Get trip by id")
    @GetMapping("/{id}")
    public TripResponse getTripById(@PathVariable Long id) {
        log.info("Fetching trip with id={}", id);
        return tripService.getTripById(id);
    }

    @Operation(summary = "Create a new trip")
    @PostMapping
    public TripResponse addTrip(@Valid @RequestBody CreateTripRequest request) {
        log.info("Creating trip: {}", request.getName());
        return tripService.addTrip(request);
    }

    @Operation(summary = "Update a trip")
    @PutMapping("/{id}")
    public TripResponse updateTrip(@PathVariable Long id, @Valid @RequestBody UpdateTripRequest request) {
        log.info("Updating trip with id={}", id);
        return tripService.updateTrip(id, request);
    }

    @Operation(summary = "Delete a trip")
    @DeleteMapping("/{id}")
    public TripResponse deleteTrip(@PathVariable Long id) {
        log.info("Deleting trip with id={}", id);
        return tripService.deleteTrip(id);
    }
}