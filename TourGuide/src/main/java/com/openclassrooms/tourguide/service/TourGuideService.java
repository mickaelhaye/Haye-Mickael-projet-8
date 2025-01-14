package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import rewardCentral.RewardCentral;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	private final ExecutorService executorService = Executors.newFixedThreadPool(500);

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocationWithReturn(user);
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void shutDowExecutorService() {
		executorService.shutdown();
	}

	public VisitedLocation trackUserLocationWithReturn(User user){
		return trackUserLocationOtherThread(user, true);
	}

	public void trackUserLocationWithoutReturn(User user){
		trackUserLocationOtherThread(user, false);
	}
	public VisitedLocation trackUserLocationOtherThread(User user, Boolean returnVisitedLocation) {
		CompletableFuture<VisitedLocation> trackUser = CompletableFuture.supplyAsync(() -> {
					VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
					return visitedLocation;
				}, executorService)
				.thenApply(visitedLocation -> {
					user.addToVisitedLocations(visitedLocation);
					return visitedLocation;
				})
				.thenApply(visitedLocation -> {
					rewardsService.calculateRewards(user);
					//logger.info("adding user = " + user.getUserName());
					return visitedLocation;
				});
		if(returnVisitedLocation) {
			return trackUser.join();
		}
		return null;
	}

	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		List<Attraction> nearbyAttractions = new ArrayList<>();

		// récupération des attractions classées du plus proche au plus loin dans un
		// TreeMap
		Map<Double, Attraction> map = new TreeMap<Double, Attraction>();
		for (Attraction attraction : gpsUtil.getAttractions()) {
			map.put(getDistanceWithinAttractionProximity(attraction, visitedLocation),
					attraction);
		}

		// recupération des 5 plus proches
		int iCpt = 0;
		for (Map.Entry<Double, Attraction> entry : map.entrySet()) {
			nearbyAttractions.add(entry.getValue());

			iCpt++;
			if (iCpt >= 5) {
				break;
			}
		}


		return nearbyAttractions;
	}

	public double getDistanceWithinAttractionProximity (Attraction attraction, VisitedLocation visitedLocation){
		return rewardsService.getDistance(attraction, visitedLocation.location);
	}

	public int getRewardPoints(Attraction attraction, User user){
		return rewardsService.getRewardPoints(attraction,user);
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	public RewardsService getRewardsService() {
		return rewardsService;
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
