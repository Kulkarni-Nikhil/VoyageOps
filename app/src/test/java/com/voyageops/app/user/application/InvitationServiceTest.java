package com.voyageops.app.user.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.domain.TripStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TripMemberService tripMemberService;

    private InvitationService invitationService;

    private final UUID ownerId = UUID.randomUUID();
    private final User owner = User.builder().id(ownerId).name("Alice").email("alice@example.com").build();
    private final Trip trip = Trip.builder()
            .id(1L)
            .name("Tokyo Trip")
            .owner(owner)
            .status(TripStatus.PLANNING)
            .build();

    @BeforeEach
    void setUp() {
        invitationService = new InvitationService(
                invitationRepository, tripRepository, tripMemberRepository,
                userRepository, tripMemberService);
    }

    @Test
    void createInvitation_setsPendingWith48hExpiry() {
        InvitationRequest request = InvitationRequest.builder()
                .invitedEmail("bob@example.com")
                .build();

        when(tripMemberService.requireOwner(ownerId, 1L)).thenReturn(owner);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(invitationRepository.existsByTripAndInvitedEmailAndStatus(
                trip, "bob@example.com", InvitationStatus.PENDING)).thenReturn(false);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> {
            Invitation invitation = inv.getArgument(0);
            invitation.setId(UUID.randomUUID());
            return invitation;
        });

        InvitationResponse response = invitationService.createInvitation(1L, ownerId, request);

        assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(response.getTripId()).isEqualTo(1L);
        assertThat(response.getTripName()).isEqualTo("Tokyo Trip");
        assertThat(response.getInvitedByName()).isEqualTo("Alice");
        assertThat(response.getInvitedEmail()).isEqualTo("bob@example.com");
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getExpiresAt())
                .isAfter(Instant.now().plus(Duration.ofHours(47)))
                .isBefore(Instant.now().plus(Duration.ofHours(49)));
    }

    @Test
    void createInvitation_rejectsDuplicatePendingInvitation() {
        InvitationRequest request = InvitationRequest.builder()
                .invitedEmail("bob@example.com")
                .build();

        when(tripMemberService.requireOwner(ownerId, 1L)).thenReturn(owner);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(invitationRepository.existsByTripAndInvitedEmailAndStatus(
                trip, "bob@example.com", InvitationStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> invitationService.createInvitation(1L, ownerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending invitation");
    }

    @Test
    void createInvitation_rejectsExistingMember() {
        UUID bobId = UUID.randomUUID();
        User bob = User.builder().id(bobId).name("Bob").email("bob@example.com").build();
        InvitationRequest request = InvitationRequest.builder()
                .invitedEmail("bob@example.com")
                .build();

        when(tripMemberService.requireOwner(ownerId, 1L)).thenReturn(owner);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(invitationRepository.existsByTripAndInvitedEmailAndStatus(
                trip, "bob@example.com", InvitationStatus.PENDING)).thenReturn(false);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(bob));
        when(tripMemberRepository.existsByTripAndUser(trip, bob)).thenReturn(true);

        assertThatThrownBy(() -> invitationService.createInvitation(1L, ownerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void acceptInvitation_createsMemberAndMarksAccepted() {
        UUID bobId = UUID.randomUUID();
        User bob = User.builder().id(bobId).name("Bob").email("bob@example.com").build();
        Invitation invitation = buildInvitation("bob@example.com", InvitationStatus.PENDING,
                Instant.now().plus(Duration.ofHours(24)));

        when(tripMemberService.requireActor(bobId)).thenReturn(bob);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));
        when(tripMemberRepository.existsByTripAndUser(trip, bob)).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationResponse response = invitationService.acceptInvitation(invitation.getToken(), bobId);

        assertThat(response.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(tripMemberRepository).save(any(TripMember.class));
    }

    @Test
    void acceptInvitation_expiredMarksExpiredAndThrows() {
        UUID bobId = UUID.randomUUID();
        User bob = User.builder().id(bobId).name("Bob").email("bob@example.com").build();
        Invitation invitation = buildInvitation("bob@example.com", InvitationStatus.PENDING,
                Instant.now().minus(Duration.ofHours(1)));

        when(tripMemberService.requireActor(bobId)).thenReturn(bob);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(invitation.getToken(), bobId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }

    @Test
    void acceptInvitation_wrongEmailThrows() {
        Invitation invitation = buildInvitation("bob@example.com", InvitationStatus.PENDING,
                Instant.now().plus(Duration.ofHours(24)));

        when(tripMemberService.requireActor(ownerId)).thenReturn(owner);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(invitation.getToken(), ownerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not addressed");
    }

    @Test
    void acceptInvitation_alreadyProcessedThrows() {
        UUID bobId = UUID.randomUUID();
        User bob = User.builder().id(bobId).name("Bob").email("bob@example.com").build();
        Invitation invitation = buildInvitation("bob@example.com", InvitationStatus.DECLINED,
                Instant.now().plus(Duration.ofHours(24)));

        when(tripMemberService.requireActor(bobId)).thenReturn(bob);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptInvitation(invitation.getToken(), bobId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already declined");
    }

    @Test
    void declineInvitation_marksDeclined() {
        UUID bobId = UUID.randomUUID();
        User bob = User.builder().id(bobId).name("Bob").email("bob@example.com").build();
        Invitation invitation = buildInvitation("bob@example.com", InvitationStatus.PENDING,
                Instant.now().plus(Duration.ofHours(24)));

        when(tripMemberService.requireActor(bobId)).thenReturn(bob);
        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InvitationResponse response = invitationService.declineInvitation(invitation.getToken(), bobId);

        assertThat(response.getStatus()).isEqualTo(InvitationStatus.DECLINED);
    }

    @Test
    void expireOverdueInvitations_marksOnlyOverduePending() {
        Invitation overdue = buildInvitation("bob@example.com", InvitationStatus.PENDING,
                Instant.now().minus(Duration.ofHours(1)));
        Invitation valid = buildInvitation("carol@example.com", InvitationStatus.PENDING,
                Instant.now().plus(Duration.ofHours(24)));

        when(invitationRepository.findByStatusAndExpiresAtBefore(eq(InvitationStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(overdue));
        when(invitationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int count = invitationService.expireOverdueInvitations();

        assertThat(count).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        assertThat(valid.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    private Invitation buildInvitation(String email, InvitationStatus status, Instant expiresAt) {
        return Invitation.builder()
                .id(UUID.randomUUID())
                .invitedBy(owner)
                .trip(trip)
                .status(status)
                .token(UUID.randomUUID().toString())
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .invitedEmail(email)
                .build();
    }
}
