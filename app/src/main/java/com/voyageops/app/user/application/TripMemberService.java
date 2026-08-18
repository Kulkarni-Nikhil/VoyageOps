package com.voyageops.app.user.application;

import com.voyageops.app.common.exception.AccessDeniedException;
import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.common.exception.UnauthorizedException;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.user.domain.MemberRole;
import com.voyageops.app.user.domain.MembershipPolicy;
import com.voyageops.app.user.domain.TripMember;
import com.voyageops.app.user.domain.User;
import com.voyageops.app.user.infrastructure.TripMemberRepository;
import com.voyageops.app.user.infrastructure.UserRepository;
import com.voyageops.app.user.web.dto.TripMemberRequest;
import com.voyageops.app.user.web.dto.TripMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripMemberService {

    private final TripMemberRepository tripMemberRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final MembershipPolicy membershipPolicy;

    @Transactional(readOnly = true)
    public List<TripMemberResponse> listMembers(Long tripId, UUID actorId) {
        Trip trip = getTrip(tripId);
        requireMember(actorId, trip);
        return tripMemberRepository.findByTrip(trip).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TripMemberResponse addMember(Long tripId, UUID actorId, TripMemberRequest request) {
        requireOwner(actorId, tripId);
        membershipPolicy.validateAssignableRole(request.getRole());
        Trip trip = getTrip(tripId);
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        if (tripMemberRepository.existsByTripAndUser(trip, user)) {
            throw new BusinessException(HttpStatus.CONFLICT, "User is already a member of this trip");
        }
        TripMember member = TripMember.builder()
                .trip(trip)
                .user(user)
                .role(request.getRole())
                .joinedAt(Instant.now())
                .build();
        return toResponse(tripMemberRepository.save(member));
    }

    @Transactional
    public TripMemberResponse changeRole(Long tripId, UUID actorId, UUID userId, MemberRole newRole) {
        requireOwner(actorId, tripId);
        Trip trip = getTrip(tripId);
        TripMember member = getMember(trip, userId);
        membershipPolicy.validateRoleChange(member.getRole(), newRole);
        member.setRole(newRole);
        return toResponse(tripMemberRepository.save(member));
    }

    @Transactional
    public void removeMember(Long tripId, UUID actorId, UUID userId) {
        requireOwner(actorId, tripId);
        Trip trip = getTrip(tripId);
        TripMember member = getMember(trip, userId);
        membershipPolicy.validateRemoval(member.getRole());
        tripMemberRepository.delete(member);
    }

    public User requireOwner(UUID actorId, Long tripId) {
        return requireOwner(actorId, getTrip(tripId));
    }

    public User requireMember(UUID actorId, Long tripId) {
        return requireMember(actorId, getTrip(tripId));
    }

    public User requireEditor(UUID actorId, Long tripId) {
        return requireEditor(actorId, getTrip(tripId));
    }

    public User requireActor(UUID actorId) {
        if (actorId == null) {
            throw new UnauthorizedException("Authentication required: provide an X-User-Id header");
        }
        return userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorId));
    }

    private User requireOwner(UUID actorId, Trip trip) {
        User actor = requireActor(actorId);
        if (!isOwner(actor, trip)) {
            throw new AccessDeniedException("Only the trip owner can perform this action");
        }
        return actor;
    }

    private User requireMember(UUID actorId, Trip trip) {
        User actor = requireActor(actorId);
        if (!isMember(actor, trip)) {
            throw new AccessDeniedException("You are not a member of this trip");
        }
        return actor;
    }

    private User requireEditor(UUID actorId, Trip trip) {
        User actor = requireActor(actorId);
        if (!isEditor(actor, trip)) {
            throw new AccessDeniedException("Only trip editors can perform this action");
        }
        return actor;
    }

    private boolean isEditor(User actor, Trip trip) {
        if (isOwner(actor, trip)) {
            return true;
        }
        return tripMemberRepository.findByTripAndUser(trip, actor)
                .map(member -> member.getRole() == MemberRole.EDITOR)
                .orElse(false);
    }

    private boolean isMember(User actor, Trip trip) {
        return isOwner(actor, trip) || tripMemberRepository.existsByTripAndUser(trip, actor);
    }

    private boolean isOwner(User actor, Trip trip) {
        return trip.getOwner() != null && trip.getOwner().getId().equals(actor.getId());
    }

    private TripMember getMember(Trip trip, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return tripMemberRepository.findByTripAndUser(trip, user)
                .orElseThrow(() -> new ResourceNotFoundException("TripMember", userId));
    }

    private Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
    }

    private TripMemberResponse toResponse(TripMember member) {
        User user = member.getUser();
        return TripMemberResponse.builder()
                .id(member.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
