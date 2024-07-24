package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.concurrent.*;

import lombok.Setter;
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
	@Setter
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewardsForAllUsers(List<User> allUsers) {
		List<CompletableFuture<Void>> completableFutureList = allUsers.stream().map(user ->  CompletableFuture.runAsync(()-> {
				calculateRewards(user);
			})).toList();

		CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[completableFutureList.size()]))
				.thenApply(v -> completableFutureList.stream().map(CompletableFuture::join)).join();
//		CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[completableFutureList.size()])).get();
//		return CompletableFuture.allOf(allUsers).thenApply(user -> calculateRewards(user)).get();
	}

	public void calculateRewards(User user) {
        List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();

		userLocations.stream()
			.flatMap(visitedLocation -> attractions.stream()
				.filter(attraction -> user.getUserRewards().stream()
						.filter(reward -> reward.attraction.attractionName.equals(attraction.attractionName)).count() == 0)
				.filter(attraction -> nearAttraction(visitedLocation, attraction))
				.map(attraction -> new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user))))
			.forEach(user::addUserReward);
	}
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) < attractionProximityRange;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) < proximityBuffer;
	}
	
	private int getRewardPoints(Attraction attraction, User user) {
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
