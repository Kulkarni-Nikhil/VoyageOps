package com.voyageops.app.trip.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.common.exception.ResourceNotFoundException;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.domain.TripStatus;
import com.voyageops.app.trip.domain.TripStatusPolicy;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.trip.web.dto.CreateTripRequest;
import com.voyageops.app.trip.web.dto.TripResponse;
import com.voyageops.app.trip.web.dto.UpdateTripRequest;
import com.voyageops.app.user.domain.User;
import com.voyageops.app.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    private final TripStatusPolicy statusPolicy = new TripStatusPolicy();
    private TripService tripService;

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, userRepository, statusPolicy);
    }

    // --- create ---

    @Test
    void addTrip_persistsPlanningTrip() {
        CreateTripRequest request = CreateTripRequest.builder()
                .name("Tokyo Trip")
                .description("First Japan trip")
                .budget(new BigDecimal("1000.00"))
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 10))
                .build();

        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        TripResponse response = tripService.addTrip(request);

        assertThat(response.getName()).isEqualTo("Tokyo Trip");
        assertThat(response.getBudget()).isEqualByComparingTo("1000.00");
        assertThat(response.getStatus()).isEqualTo(TripStatus.PLANNING);
        assertThat(response.getOwnerId()).isNull();
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void addTrip_resolvesOwner() {
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).name("Alice").email("alice@example.com").build();
        CreateTripRequest request = CreateTripRequest.builder()
                .name("Tokyo Trip")
                .ownerId(ownerId)
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 10))
                .build();

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        TripResponse response = tripService.addTrip(request);

        assertThat(response.getOwnerId()).isEqualTo(ownerId);
        verify(userRepository).findById(ownerId);
    }

    @Test
    void addTrip_throwsWhenOwnerDoesNotExist() {
        UUID ownerId = UUID.randomUUID();
        CreateTripRequest request = CreateTripRequest.builder()
                .name("Tokyo Trip")
                .ownerId(ownerId)
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 10))
                .build();

        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.addTrip(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void addTrip_rejectsEndDateBeforeStartDate() {
        CreateTripRequest request = CreateTripRequest.builder()
                .name("Broken Trip")
                .startDate(LocalDate.of(2026, 9, 10))
                .endDate(LocalDate.of(2026, 9, 1))
                .build();

        assertThatThrownBy(() -> tripService.addTrip(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("End date cannot be before start date");
    }

    // --- read ---

    @Test
    void getTripById_returnsResponse() {
        Trip trip = buildTrip();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        TripResponse response = tripService.getTripById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Tokyo Trip");
    }

    @Test
    void getTripById_throwsWhenNotFound() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTripById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trip not found");
    }

    // --- update ---

    @Test
    void updateTrip_appliesChangesAndValidTransition() {
        Trip trip = buildTrip();
        UpdateTripRequest request = UpdateTripRequest.builder()
                .name("Updated Trip")
                .budget(new BigDecimal("2500.00"))
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 15))
                .status(TripStatus.PLANNED)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        TripResponse response = tripService.updateTrip(1L, request);

        assertThat(response.getName()).isEqualTo("Updated Trip");
        assertThat(response.getBudget()).isEqualByComparingTo("2500.00");
        assertThat(response.getStatus()).isEqualTo(TripStatus.PLANNED);
    }

    @Test
    void updateTrip_rejectsInvalidStatusTransition() {
        Trip trip = buildTrip();
        UpdateTripRequest request = UpdateTripRequest.builder()
                .name("Tokyo Trip")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 10))
                .status(TripStatus.COMPLETED)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.updateTrip(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid trip status transition");
    }

    @Test
    void updateTrip_rejectsChangesToTerminalTrip() {
        Trip trip = buildTrip();
        trip.setStatus(TripStatus.COMPLETED);
        UpdateTripRequest request = UpdateTripRequest.builder()
                .name("Hacked")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 10))
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.updateTrip(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be modified");
    }

    @Test
    void updateTrip_rejectsEndDateBeforeStartDate() {
        Trip trip = buildTrip();
        UpdateTripRequest request = UpdateTripRequest.builder()
                .name("Tokyo Trip")
                .startDate(LocalDate.of(2026, 9, 15))
                .endDate(LocalDate.of(2026, 9, 1))
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.updateTrip(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("End date cannot be before start date");
    }

    // --- delete ---

    @Test
    void deleteTrip_deletesAndReturnsTrip() {
        Trip trip = buildTrip();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        TripResponse response = tripService.deleteTrip(1L);

        assertThat(response.getId()).isEqualTo(1L);
        verify(tripRepository).delete(trip);
    }

    @Test
    void deleteTrip_throwsWhenNotFound() {
        when(tripRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.deleteTrip(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Trip buildTrip() {
        return Trip.builder()
                .id(1L)
                .name("Tokyo Trip")
                .description("First Japan trip")
                .budget(new BigDecimal("1000.00"))
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 10))
                .status(TripStatus.PLANNING)
                .build();
    }
}