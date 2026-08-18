package com.voyageops.app.weather.domain;

import java.util.Optional;

/**
 * Resolves a free-text location into coordinates.
 * <p>
 * Implemented against Open-Meteo's geocoding API and shared by the weather and
 * places features.
 */
public interface Geocoder {

    Optional<GeoCoordinate> geocode(String query);
}
