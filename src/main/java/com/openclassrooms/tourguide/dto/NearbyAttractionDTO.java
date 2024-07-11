package com.openclassrooms.tourguide.dto;

import gpsUtil.location.Location;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class NearbyAttractionDTO {
    private String attractionName;
    private Location attractionLocation;
    private Location userLocation;
    private double distanceInMiles;
    private int rewardPoints;
}
