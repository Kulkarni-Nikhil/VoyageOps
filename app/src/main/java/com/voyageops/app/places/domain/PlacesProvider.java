package com.voyageops.app.places.domain;

import com.voyageops.app.weather.domain.GeoCoordinate;

import java.util.List;

/**
 * Finds nearby points of interest around a location.
 */
public interface PlacesProvider {

    List<Place> findNearby(GeoCoordinate coordinate, int radiusMeters, int limit);
}
