package com.openclassrooms.tourguide.modelDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import lombok.Data;

@Data
public class NearByAttractionsDto {

    @JsonProperty("attraction name")
    private String Attraction_Name;

    @JsonProperty("attraction longitude")
    private Double Attraction_Longitude;

    @JsonProperty("attraction latitude")
    private Double Attraction_Latitude;

    @JsonProperty("user longitude")
    private Double User_Longitude;

    @JsonProperty("user latitude")
    private Double User_Latitude;

    @JsonProperty("distance")
    private Double Distance;

    @JsonProperty("rewards points")
    private int RewardPoints;



    public NearByAttractionsDto(Attraction attraction, VisitedLocation visitedLocation, Double distance,
                                int rewardPoints) {

        this.Attraction_Name = attraction.attractionName;
        this.Attraction_Longitude = attraction.longitude;
        this.Attraction_Latitude = attraction.latitude;
        this.User_Longitude = visitedLocation.location.longitude;
        this.User_Latitude = visitedLocation.location.latitude;
        this.Distance = distance;
        this.RewardPoints = rewardPoints;
    }
}