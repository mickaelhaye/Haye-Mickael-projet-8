package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private final List<Attraction> attractions;
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
		this.attractions = gpsUtil.getAttractions();
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}
	
	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = new ArrayList<>(user.getVisitedLocations());
		List<Attraction> attractionNotDo = new ArrayList<>(attractions);
		List<Attraction> attractionAlreadyDo = new ArrayList<>(user.getUserRewards().stream().map(userReward -> userReward.attraction).collect(Collectors.toList()));

		for(VisitedLocation visitedLocation : userLocations) {
			//System.out.println(Thread.currentThread().getName()+" userName="+user.getUserName()+" Reawards="+user.getUserRewards().size() +" étape1");
			attractionNotDo.removeAll(attractionAlreadyDo);
			attractionAlreadyDo.clear();
			for(Attraction attraction : attractionNotDo) {
				//System.out.println(Thread.currentThread().getName()+" userName="+user.getUserName()+" Reawards="+user.getUserRewards().size() +" étape2" );
				if(nearAttraction(visitedLocation, attraction)) {
					//System.out.println(Thread.currentThread().getName()+" userName="+user.getUserName()+" Reawards="+user.getUserRewards().size() +" rajout add0");
					user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					//System.out.println(Thread.currentThread().getName()+" userName="+user.getUserName()+" Reawards="+user.getUserRewards().size() +" rajout add1");
					attractionAlreadyDo.add(attraction);
				}
			}
		}
	}
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		//System.out.println(Thread.currentThread().getName()+ " calculdistance="+proximityBuffer);
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
