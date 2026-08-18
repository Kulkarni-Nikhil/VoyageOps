package com.voyageops.app.places.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceResponse {
    private String xid;
    private String name;
    private String kinds;
    private Double distanceMeters;
    private Double rate;
}
