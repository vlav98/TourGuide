package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.dto.NearbyAttractionDTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import tripPricer.Provider;

public class TestTourGuideService {
    private TourGuideService tourGuideService;
	private User user;

	@BeforeEach
	public void initEach() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
	}

	@Test
	public void getUserLocation() throws ExecutionException, InterruptedException {
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user).get();
		tourGuideService.tracker.stopTracking();
        assertEquals(visitedLocation.userId, user.getUserId());
	}

	@Test
	public void addUser() {
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);

		User retrivedUser = tourGuideService.getUser(user.getUserName());
		User retrivedUser2 = tourGuideService.getUser(user2.getUserName());

		tourGuideService.tracker.stopTracking();

		assertEquals(user, retrivedUser);
		assertEquals(user2, retrivedUser2);
	}

	@Test
	public void getAllUsers() {
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);

		List<User> allUsers = tourGuideService.getAllUsers();

		tourGuideService.tracker.stopTracking();

		assertTrue(allUsers.contains(user));
		assertTrue(allUsers.contains(user2));
	}

	@Test
	public void trackUser() throws ExecutionException, InterruptedException {
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user).get();

		tourGuideService.tracker.stopTracking();

		assertEquals(user.getUserId(), visitedLocation.userId);
	}

	@Test
	public void getNearbyAttractions() throws ExecutionException, InterruptedException {
		tourGuideService.addUser(user);
		List<NearbyAttractionDTO> attractions = tourGuideService.getNearByAttractions(user.getUserName());

		tourGuideService.tracker.stopTracking();

		assertEquals(5, attractions.size());
	}

	@Test
	public void getTripDeals() {
		List<Provider> providers = tourGuideService.getTripDeals(user);

		tourGuideService.tracker.stopTracking();

		assertEquals(5, providers.size());
	}

}
