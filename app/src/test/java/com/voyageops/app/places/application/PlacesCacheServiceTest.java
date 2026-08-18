package com.voyageops.app.places.application;

import com.voyageops.app.common.exception.BusinessException;
import com.voyageops.app.places.domain.Place;
import com.voyageops.app.places.domain.PlacesProvider;
import com.voyageops.app.places.web.dto.TripPlacesResponse;
import com.voyageops.app.trip.application.TripDestinationResolver;
import com.voyageops.app.trip.domain.Destination;
import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.trip.infrastructure.TripRepository;
import com.voyageops.app.weather.domain.GeoCoordinate;
import com.voyageops.app.weather.domain.Geocoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacesCacheServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDestinationResolver destinationResolver;

    @Mock
    private Geocoder geocoder;

    @Mock
    private PlacesProvider placesProvider;

    private PlacesCacheService placesCacheService;

    @BeforeEach
    void setUp() {
        placesCacheService = new PlacesCacheService(tripRepository, destinationResolver, geocoder, placesProvider);
    }

    @Test
    void getTripPlaces_mapsPlacesAndClampsRadius() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(Trip.builder().id(1L).build()));
        Destination destination = Destination.builder().city("Tokyo").country("Japan").build();
        when(destinationResolver.primary(1L)).thenReturn(destination);
        when(destinationResolver.label(destination)).thenReturn("Tokyo, Japan");
        when(geocoder.geocode("Tokyo, Japan"))
                .thenReturn(Optional.of(new GeoCoordinate(35.68, 139.69)));
        when(placesProvider.findNearby(any(GeoCoordinate.class), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of(new Place("xid-1", "Tokyo Tower", "towers", 1200.0, 4.5)));

        TripPlacesResponse response = placesCacheService.getTripPlaces(1L, 100_000);

        assertThat(response.getRadiusMeters()).isEqualTo(50_000);
        assertThat(response.getPlaces()).hasSize(1);
        assertThat(response.getPlaces().get(0).getName()).isEqualTo("Tokyo Tower");
        assertThat(response.getPlaces().get(0).getDistanceMeters()).isEqualTo(1200.0);
        assertThat(response.getPlaces().get(0).getRate()).isEqualTo(4.5);
    }

    @Test
    void getTripPlaces_clampsTinyRadiusToMinimum() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(Trip.builder().id(1L).build()));
        Destination destination = Destination.builder().city("Tokyo").build();
        when(destinationResolver.primary(1L)).thenReturn(destination);
        when(destinationResolver.label(destination)).thenReturn("Tokyo");
        when(geocoder.geocode("Tokyo")).thenReturn(Optional.of(new GeoCoordinate(35.68, 139.69)));
        when(placesProvider.findNearby(any(GeoCoordinate.class), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of());

        TripPlacesResponse response = placesCacheService.getTripPlaces(1L, 50);

        assertThat(response.getRadiusMeters()).isEqualTo(100);
    }

    @Test
    void getTripPlaces_failsWhenLocationCannotBeResolved() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(Trip.builder().id(1L).build()));
        Destination destination = Destination.builder().city("Nowhere").build();
        when(destinationResolver.primary(1L)).thenReturn(destination);
        when(destinationResolver.label(destination)).thenReturn("Nowhere");
        when(geocoder.geocode("Nowhere")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> placesCacheService.getTripPlaces(1L, 5000))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Could not resolve location");
    }
}
