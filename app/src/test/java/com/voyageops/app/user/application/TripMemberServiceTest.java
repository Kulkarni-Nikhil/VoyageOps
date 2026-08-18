package com.voyageops.app.user.application;

import com.voyageops.app.common.exception.AccessDeniedException;
import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.common.exception.UnauthorizedException;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.domain.TripStatus;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.user.domain.MemberRole;
import com.voyageops.app.user.domain.MembershipPolicy;
import com.voyageops.app.user.domain.TripMember;
import com.voyageops.app.user.domain.User;
import com.voyageops.app.user.infrastructure.TripMemberRepository;
import com.voyageops.app.user.infrastructure.UserRepository;
import com.voyageops.app.user.web.dto.TripMemberRequest;
import com.voyageops.app.user.web.dto.TripMemberResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripMemberServiceTest {

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    private final MembershipPolicy membershipPolicy = new MembershipPolicy();
    private TripMemberService tripMemberService;

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
        tripMemberService = new TripMemberService(
                tripMemberRepository, tripRepository, userRepository, membershipPolicy);
    }

    @Test
    void addMember_ownerAddsMember() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).name("Bob").email("bob@example.com").build();
        TripMemberRequest request = TripMemberRequest.builder()
                .userId(userId)
                .role(MemberRole.EDITOR)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tripMemberRepository.existsByTripAndUser(trip, user)).thenReturn(false);
        when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(inv -> {
            TripMember member = inv.getArgument(0);
            member.setId(UUID.randomUUID());
            return member;
        });

        TripMemberResponse response = tripMemberService.addMember(1L, ownerId, request);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo(MemberRole.EDITOR);
        assertThat(response.getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    void addMember_nonOwnerThrows() {
        UUID nonOwnerId = UUID.randomUUID();
        User nonOwner = User.builder().id(nonOwnerId).name("Bob").email("bob@example.com").build();
        TripMemberRequest request = TripMemberRequest.builder()
                .userId(UUID.randomUUID())
                .role(MemberRole.MEMBER)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(nonOwnerId)).thenReturn(Optional.of(nonOwner));

        assertThatThrownBy(() -> tripMemberService.addMember(1L, nonOwnerId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addMember_rejectsOwnerRole() {
        TripMemberRequest request = TripMemberRequest.builder()
                .userId(UUID.randomUUID())
                .role(MemberRole.OWNER)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> tripMemberService.addMember(1L, ownerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OWNER");
    }

    @Test
    void addMember_duplicateMemberThrows() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).name("Bob").email("bob@example.com").build();
        TripMemberRequest request = TripMemberRequest.builder()
                .userId(userId)
                .role(MemberRole.MEMBER)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tripMemberRepository.existsByTripAndUser(trip, user)).thenReturn(true);

        assertThatThrownBy(() -> tripMemberService.addMember(1L, ownerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void addMember_unknownUserThrows() {
        UUID userId = UUID.randomUUID();
        TripMemberRequest request = TripMemberRequest.builder()
                .userId(userId)
                .role(MemberRole.MEMBER)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripMemberService.addMember(1L, ownerId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void addMember_withoutActorThrows() {
        TripMemberRequest request = TripMemberRequest.builder()
                .userId(UUID.randomUUID())
                .role(MemberRole.MEMBER)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripMemberService.addMember(1L, null, request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    void changeRole_ownerUpdatesRole() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).name("Bob").email("bob@example.com").build();
        TripMember member = TripMember.builder()
                .id(UUID.randomUUID())
                .trip(trip)
                .user(user)
                .role(MemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tripMemberRepository.findByTripAndUser(trip, user)).thenReturn(Optional.of(member));
        when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TripMemberResponse response = tripMemberService.changeRole(1L, ownerId, userId, MemberRole.EDITOR);

        assertThat(response.getRole()).isEqualTo(MemberRole.EDITOR);
    }

    @Test
    void changeRole_cannotDemoteOwner() {
        TripMember ownerMember = TripMember.builder()
                .id(UUID.randomUUID())
                .trip(trip)
                .user(owner)
                .role(MemberRole.OWNER)
                .joinedAt(Instant.now())
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(tripMemberRepository.findByTripAndUser(trip, owner)).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> tripMemberService.changeRole(1L, ownerId, ownerId, MemberRole.MEMBER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("owner's role");
    }

    @Test
    void changeRole_cannotPromoteToOwner() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).name("Bob").email("bob@example.com").build();
        TripMember member = TripMember.builder()
                .id(UUID.randomUUID())
                .trip(trip)
                .user(user)
                .role(MemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tripMemberRepository.findByTripAndUser(trip, user)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> tripMemberService.changeRole(1L, ownerId, userId, MemberRole.OWNER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OWNER");
    }

    @Test
    void removeMember_ownerRemovesEditor() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).name("Bob").email("bob@example.com").build();
        TripMember member = TripMember.builder()
                .id(UUID.randomUUID())
                .trip(trip)
                .user(user)
                .role(MemberRole.EDITOR)
                .joinedAt(Instant.now())
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tripMemberRepository.findByTripAndUser(trip, user)).thenReturn(Optional.of(member));

        tripMemberService.removeMember(1L, ownerId, userId);

        verify(tripMemberRepository).delete(member);
    }

    @Test
    void removeMember_cannotRemoveOwner() {
        TripMember ownerMember = TripMember.builder()
                .id(UUID.randomUUID())
                .trip(trip)
                .user(owner)
                .role(MemberRole.OWNER)
                .joinedAt(Instant.now())
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(tripMemberRepository.findByTripAndUser(trip, owner)).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> tripMemberService.removeMember(1L, ownerId, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void listMembers_nonMemberThrows() {
        UUID outsiderId = UUID.randomUUID();
        User outsider = User.builder().id(outsiderId).name("Eve").email("eve@example.com").build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(outsiderId)).thenReturn(Optional.of(outsider));
        when(tripMemberRepository.existsByTripAndUser(trip, outsider)).thenReturn(false);

        assertThatThrownBy(() -> tripMemberService.listMembers(1L, outsiderId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listMembers_ownerSeesAllMembers() {
        UUID bobId = UUID.randomUUID();
        User bob = User.builder().id(bobId).name("Bob").email("bob@example.com").build();
        TripMember member = TripMember.builder()
                .id(UUID.randomUUID())
                .trip(trip)
                .user(bob)
                .role(MemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(tripMemberRepository.findByTrip(trip)).thenReturn(List.of(member));

        List<TripMemberResponse> result = tripMemberService.listMembers(1L, ownerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("bob@example.com");
        assertThat(result.get(0).getRole()).isEqualTo(MemberRole.MEMBER);
    }
}
