package com.voyageops.app.trip.web;

import com.voyageops.app.trip.application.ActivityService;
import com.voyageops.app.trip.web.dto.ActivityRequest;
import com.voyageops.app.trip.web.dto.ActivityResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Activities", description = "Trip activity endpoints")
@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping("/trips/{tripId}/activities")
    public ActivityResponse addActivity(@PathVariable Long tripId,
                                        @Valid @RequestBody ActivityRequest request) {
        log.info("Adding activity to trip {}", tripId);
        return activityService.addActivity(tripId, request);
    }

    @GetMapping("/trips/{tripId}/activities")
    public List<ActivityResponse> getActivitiesByTrip(@PathVariable Long tripId) {
        log.info("Fetching activities for trip {}", tripId);
        return activityService.getActivitiesByTrip(tripId);
    }

    @GetMapping("/activities/{id}")
    public ActivityResponse getActivityById(@PathVariable Long id) {
        log.info("Fetching activity {}", id);
        return activityService.getActivityById(id);
    }

    @PutMapping("/activities/{id}")
    public ActivityResponse updateActivity(@PathVariable Long id,
                                           @Valid @RequestBody ActivityRequest request) {
        log.info("Updating activity {}", id);
        return activityService.updateActivity(id, request);
    }

    @DeleteMapping("/activities/{id}")
    public ActivityResponse deleteActivity(@PathVariable Long id) {
        log.info("Deleting activity {}", id);
        return activityService.deleteActivity(id);
    }
}