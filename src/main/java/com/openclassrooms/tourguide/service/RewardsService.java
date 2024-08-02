package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
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
	private final ExecutorService executorService = Executors.newFixedThreadPool(200);
	private final List<Attraction> attractionList = new ArrayList<>();

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;

		init();
	}

	public void init() {
		attractionList.addAll(gpsUtil.getAttractions());
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewardsForAllUsers(List<User> allUsers) {
		List<CompletableFuture<Void>> listRewards = new ArrayList<>();
		allUsers.parallelStream().forEach(u -> listRewards.add(calculateRewards(u)));
		listRewards.parallelStream().forEach(CompletableFuture::join);
	}

	public CompletableFuture<Void> calculateRewards(User user) {
		return CompletableFuture.runAsync(() -> {
        	List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());

			userLocations.forEach(visitedLocation -> attractionList.stream()
				.filter(attraction -> (nearAttraction(visitedLocation, attraction)) &&
					user.getUserRewards().stream()
							.noneMatch(reward -> reward.attraction.attractionName.equals(attraction.attractionName)))
				.forEach(attraction -> addUserReward(user, attraction, visitedLocation)));
		}, executorService);
	}

	private void addUserReward(User user, Attraction attraction, VisitedLocation visitedLocation) {
		UserReward userReward = new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user));
		user.addUserReward(userReward);
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
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}

}
