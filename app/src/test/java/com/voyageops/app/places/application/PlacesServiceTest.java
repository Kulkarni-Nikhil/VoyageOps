package com.voyageops.app.places.application;

import com.voyageops.app.places.web.dto.TripPlacesResponse;
import com.voyageops.app.user.application.TripMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacesServiceTest {

    @Mock
    private TripMemberService tripMemberService;

    @Mock
    private PlacesCacheService placesCacheService;

    private PlacesService placesService;

    @BeforeEach
    void setUp() {
        placesService = new PlacesService(tripMemberService, placesCacheService);
    }

    @Test
    void getTripPlaces_enforcesMembershipThenDelegates() {
        UUID actorId = UUID.randomUUID();
        TripPlacesResponse response = TripPlacesResponse.builder().tripId(1L).build();
        when(placesCacheService.getTripPlaces(1L, 5000)).thenReturn(response);

        TripPlacesResponse result = placesService.getTripPlaces(1L, actorId, 5000);

        verify(tripMemberService).requireMember(actorId, 1L);
        assertThat(result.getTripId()).isEqualTo(1L);
    }
}
