package com.voyageops.app.places.web.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripPlacesResponse {
    private Long tripId;
    private String destination;
    private Integer radiusMeters;
    private Double latitude;
    private Double longitude;
    private List<PlaceResponse> places;
}
