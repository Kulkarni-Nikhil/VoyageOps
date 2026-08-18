package com.voyageops.app.trip.web;

import com.voyageops.app.trip.application.DestinationService;
import com.voyageops.app.trip.web.dto.DestinationRequest;
import com.voyageops.app.trip.web.dto.DestinationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Destinations", description = "Trip destination endpoints")
@RestController
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping("/trips/{tripId}/destinations")
    public List<DestinationResponse> getDestinationsByTrip(@PathVariable Long tripId) {
        return destinationService.getDestinationByTrip(tripId);
    }

    @GetMapping("/destinations/{id}")
    public DestinationResponse getDestinationById(@PathVariable Long id) {
        return destinationService.getDestinationById(id);
    }

    @PostMapping("/trips/{tripId}/destinations")
    public DestinationResponse addDestination(@PathVariable Long tripId,
                                              @Valid @RequestBody DestinationRequest request) {
        return destinationService.addDestination(tripId, request);
    }

    @PutMapping("/destinations/{id}")
    public DestinationResponse updateDestination(@PathVariable Long id,
                                                 @Valid @RequestBody DestinationRequest request) {
        return destinationService.updateDestination(id, request);
    }

    @DeleteMapping("/destinations/{id}")
    public DestinationResponse deleteDestination(@PathVariable Long id) {
        return destinationService.deleteDestination(id);
    }
}