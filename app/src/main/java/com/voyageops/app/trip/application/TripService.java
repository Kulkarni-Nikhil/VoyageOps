package com.voyageops.app.trip.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.common.web.PageResponse;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.domain.TripStatus;
import com.voyageops.app.trip.domain.TripStatusPolicy;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.trip.web.dto.CreateTripRequest;
import com.voyageops.app.trip.web.dto.TripResponse;
import com.voyageops.app.trip.web.dto.UpdateTripRequest;
import com.voyageops.app.user.domain.MemberRole;
import com.voyageops.app.user.domain.TripMember;
import com.voyageops.app.user.domain.User;
import com.voyageops.app.user.infrastructure.TripMemberRepository;
import com.voyageops.app.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripStatusPolicy statusPolicy;
    private final TripMemberRepository tripMemberRepository;

    @Transactional(readOnly = true)
    public PageResponse<TripResponse> getAllTrips(Pageable pageable) {
        Page<TripResponse> page = tripRepository.findAll(pageable)
                .map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public TripResponse getTripById(Long id) {
        return toResponse(getTrip(id));
    }

    @Transactional
    public TripResponse addTrip(CreateTripRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());

        User owner = resolveOwner(request.getOwnerId());

        Trip trip = Trip.builder()
                .name(request.getName())
                .description(request.getDescription())
                .budget(request.getBudget())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(TripStatus.PLANNING)
                .owner(owner)
                .build();

        Trip saved = tripRepository.save(trip);
        if (owner != null) {
            TripMember member = TripMember.builder()
                    .trip(saved)
                    .user(owner)
                    .role(MemberRole.OWNER)
                    .joinedAt(Instant.now())
                    .build();
            tripMemberRepository.save(member);
        }
        return toResponse(saved);
    }

    @Transactional
    public TripResponse updateTrip(Long id, UpdateTripRequest request) {
        Trip trip = getTrip(id);
        validateDateRange(request.getStartDate(), request.getEndDate());

        if (statusPolicy.isTerminal(trip.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Completed or cancelled trips cannot be modified");
        }
        if (request.getStatus() != null) {
            statusPolicy.validateTransition(trip.getStatus(), request.getStatus());
            trip.setStatus(request.getStatus());
        }

        trip.setName(request.getName());
        trip.setDescription(request.getDescription());
        trip.setBudget(request.getBudget());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse deleteTrip(Long id) {
        Trip trip = getTrip(id);
        tripRepository.delete(trip);
        return toResponse(trip);
    }

    private Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
    }

    private User resolveOwner(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));
    }

    private void validateDateRange(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException("End date cannot be before start date");
        }
    }

    private TripResponse toResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .name(trip.getName())
                .description(trip.getDescription())
                .budget(trip.getBudget())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .status(trip.getStatus())
                .ownerId(trip.getOwner() != null ? trip.getOwner().getId() : null)
                .version(trip.getVersion())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }
}