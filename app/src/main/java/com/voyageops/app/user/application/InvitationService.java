package com.voyageops.app.user.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.user.domain.Invitation;
import com.voyageops.app.user.domain.InvitationStatus;
import com.voyageops.app.user.domain.MemberRole;
import com.voyageops.app.user.domain.TripMember;
import com.voyageops.app.user.domain.User;
import com.voyageops.app.user.infrastructure.InvitationRepository;
import com.voyageops.app.user.infrastructure.TripMemberRepository;
import com.voyageops.app.user.infrastructure.UserRepository;
import com.voyageops.app.user.web.dto.InvitationRequest;
import com.voyageops.app.user.web.dto.InvitationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final Duration INVITATION_VALIDITY = Duration.ofHours(48);

    private final InvitationRepository invitationRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final TripMemberService tripMemberService;

    @Transactional
    public InvitationResponse createInvitation(Long tripId, UUID actorId, InvitationRequest request) {
        User actor = tripMemberService.requireOwner(actorId, tripId);
        Trip trip = getTrip(tripId);
        String email = normalizeEmail(request.getInvitedEmail());
        if (invitationRepository.existsByTripAndInvitedEmailAndStatus(trip, email, InvitationStatus.PENDING)) {
            throw new BusinessException(HttpStatus.CONFLICT, "A pending invitation already exists for this email");
        }
        if (isExistingOwnerOrMember(trip, email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "This user is already a member of the trip");
        }
        Invitation invitation = Invitation.builder()
                .invitedBy(actor)
                .trip(trip)
                .status(InvitationStatus.PENDING)
                .token(UUID.randomUUID().toString().replace("-", ""))
                .expiresAt(Instant.now().plus(INVITATION_VALIDITY))
                .createdAt(Instant.now())
                .invitedEmail(email)
                .build();
        return toResponse(invitationRepository.save(invitation));
    }

    @Transactional(readOnly = true)
    public InvitationResponse getInvitation(String token) {
        return toResponse(getInvitationEntity(token));
    }

    @Transactional
    public InvitationResponse acceptInvitation(String token, UUID actorId) {
        User actor = tripMemberService.requireActor(actorId);
        Invitation invitation = getInvitationEntity(token);
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("Invitation is already " + invitation.getStatus().name().toLowerCase());
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BusinessException("Invitation has expired");
        }
        if (!invitation.getInvitedEmail().equalsIgnoreCase(actor.getEmail())) {
            throw new BusinessException("This invitation is not addressed to the signed-in user");
        }
        if (!tripMemberRepository.existsByTripAndUser(invitation.getTrip(), actor)) {
            TripMember member = TripMember.builder()
                    .trip(invitation.getTrip())
                    .user(actor)
                    .role(MemberRole.MEMBER)
                    .joinedAt(Instant.now())
                    .build();
            tripMemberRepository.save(member);
        }
        invitation.setStatus(InvitationStatus.ACCEPTED);
        return toResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public InvitationResponse declineInvitation(String token, UUID actorId) {
        User actor = tripMemberService.requireActor(actorId);
        Invitation invitation = getInvitationEntity(token);
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("Invitation is already " + invitation.getStatus().name().toLowerCase());
        }
        if (!invitation.getInvitedEmail().equalsIgnoreCase(actor.getEmail())) {
            throw new BusinessException("This invitation is not addressed to the signed-in user");
        }
        invitation.setStatus(InvitationStatus.DECLINED);
        return toResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public int expireOverdueInvitations() {
        List<Invitation> overdue = invitationRepository.findByStatusAndExpiresAtBefore(
                InvitationStatus.PENDING, Instant.now());
        overdue.forEach(inv -> inv.setStatus(InvitationStatus.EXPIRED));
        invitationRepository.saveAll(overdue);
        return overdue.size();
    }

    private boolean isExistingOwnerOrMember(Trip trip, String email) {
        if (trip.getOwner() != null && trip.getOwner().getEmail().equalsIgnoreCase(email)) {
            return true;
        }
        return userRepository.findByEmail(email)
                .map(user -> tripMemberRepository.existsByTripAndUser(trip, user))
                .orElse(false);
    }

    private Invitation getInvitationEntity(String token) {
        return invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", token));
    }

    private Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private InvitationResponse toResponse(Invitation invitation) {
        User invitedBy = invitation.getInvitedBy();
        return InvitationResponse.builder()
                .id(invitation.getId())
                .tripId(invitation.getTrip().getId())
                .tripName(invitation.getTrip().getName())
                .invitedByName(invitedBy != null ? invitedBy.getName() : null)
                .invitedEmail(invitation.getInvitedEmail())
                .status(invitation.getStatus())
                .token(invitation.getToken())
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
