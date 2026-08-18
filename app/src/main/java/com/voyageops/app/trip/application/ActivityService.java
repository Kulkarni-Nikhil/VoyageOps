package com.voyageops.app.trip.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.config.CacheConfig;
import com.voyageops.app.trip.domain.Activity;
import com.voyageops.app.trip.domain.ActivityStatus;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.ActivityRepository;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.trip.web.dto.ActivityRequest;
import com.voyageops.app.trip.web.dto.ActivityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final TripRepository tripRepository;

    @CacheEvict(cacheNames = CacheConfig.CACHE_ACTIVITIES, key = "#tripId")
    @Transactional
    public ActivityResponse addActivity(Long tripId, ActivityRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

        validateCost(request);

        Activity activity = Activity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .cost(request.getCost())
                .status(ActivityStatus.PLANNED)
                .scheduledFor(request.getScheduledFor())
                .trip(trip)
                .build();

        return toResponse(activityRepository.save(activity));
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_ACTIVITIES, key = "#tripId")
    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivitiesByTrip(Long tripId) {
        return activityRepository.findByTripId(tripId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(Long activityId) {
        return toResponse(getActivity(activityId));
    }

    @CacheEvict(cacheNames = CacheConfig.CACHE_ACTIVITIES, key = "#result.tripId")
    @Transactional
    public ActivityResponse updateActivity(Long activityId, ActivityRequest request) {
        Activity activity = getActivity(activityId);
        validateCost(request);

        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setCost(request.getCost());
        activity.setScheduledFor(request.getScheduledFor());

        return toResponse(activityRepository.save(activity));
    }

    @CacheEvict(cacheNames = CacheConfig.CACHE_ACTIVITIES, key = "#result.tripId")
    @Transactional
    public ActivityResponse deleteActivity(Long id) {
        Activity activity = getActivity(id);
        activityRepository.delete(activity);
        return toResponse(activity);
    }

    private Activity getActivity(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", id));
    }

    private void validateCost(ActivityRequest request) {
        if (request.getCost() != null && request.getCost().signum() < 0) {
            throw new BusinessException("Activity cost cannot be negative");
        }
    }

    private ActivityResponse toResponse(Activity activity) {
        return ActivityResponse.builder()
                .id(activity.getId())
                .tripId(activity.getTrip() != null ? activity.getTrip().getId() : null)
                .title(activity.getTitle())
                .description(activity.getDescription())
                .cost(activity.getCost())
                .status(activity.getStatus())
                .scheduledFor(activity.getScheduledFor())
                .build();
    }
}