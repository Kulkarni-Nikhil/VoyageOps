package com.voyageops.app.places.domain;

public record Place(String xid, String name, String kinds, double distanceMeters, Double rate) {
}
