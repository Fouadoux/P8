package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import java.util.stream.Collectors;


import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

/**
 * Service for calculating and managing user rewards based on visited locations
 * and nearby attractions.
 */
@Setter
@Slf4j
@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
    private final ExecutorService executorService;


    // proximity in miles
    private int defaultProximityBuffer = 10;

    public int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;

    /**
     * Constructor to initialize the RewardsService.
     *
     * @param gpsUtil        Service to retrieve GPS-based attraction data
     * @param rewardCentral  Service to retrieve attraction reward points
     * @param executorService Thread pool executor for asynchronous tasks
     */
    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral,ExecutorService executorService) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
        this.executorService = executorService;
    }

    /**
     * Resets the proximity buffer to its default value.
     */
    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    /**
     * Calculates rewards for a given user based on their visited locations and nearby attractions.
     *
     * @param user The user for whom rewards should be calculated.
     */
    public synchronized void calculateRewards(User user) {
        List<VisitedLocation> userLocations = user.getVisitedLocations();
        List<Attraction> attractions = gpsUtil.getAttractions();

        for (VisitedLocation visitedLocation : userLocations) {
            for (Attraction attraction : attractions) {
                if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
                    if (nearAttraction(visitedLocation, attraction)) {
                        user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
                    }
                }
            }
        }
    }

    /**
     * Asynchronously calculates rewards for a user.
     *
     * @param user        The user for whom rewards should be calculated.
     * @param executorService The thread pool executor for handling the task asynchronously.
     * @return A CompletableFuture representing the completion of the reward calculation.
     */
	public CompletableFuture<Void> calculateRewardsAsync(User user, ExecutorService executorService,List<Attraction> attractions) {
		List<VisitedLocation> userLocations = user.getVisitedLocations(); // Liste des localisations visitées

		return CompletableFuture.runAsync(() -> {

			try {
				synchronized (user) {
					userLocations.forEach(visitedLocation -> {
						attractions.forEach(attraction -> {
							// Vérifier si une récompense existe déjà pour cette attraction
							boolean rewardExists = user.getUserRewards().stream()
									.anyMatch(r -> r.attraction.attractionName.equals(attraction.attractionName));

							if (!rewardExists && nearAttraction(visitedLocation, attraction)) {
								// Ajouter une nouvelle récompense
								user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
							}
						});
					});
				}
			} catch (Exception e) {
				log.error("Error while calculating rewards asynchronously for user '{}': {}", user.getUserName(), e.getMessage(), e);
				throw new RuntimeException("Error while calculating rewards asynchronously", e);
			}
		}, executorService);
	}

    /**
     * Tracks and calculates rewards for all users asynchronously.
     *
     * @param users The list of users for whom rewards should be calculated.
     */
    public void trackCalculateRewardsAsync(List<User> users) {
        List<Attraction> attractions = gpsUtil.getAttractions();

        try {
            List<CompletableFuture<Void>> futures = users.stream()
                    .map(user -> calculateRewardsAsync(user, executorService,attractions))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Error during tracking: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks if a given location is within the proximity of an attraction.
     *
     * @param attraction The attraction to check.
     * @param location   The location of interest.
     * @return True if the location is within proximity, false otherwise.
     */
    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    /**
     * Determines if a visited location is near an attraction based on the proximity buffer.
     *
     * @param visitedLocation The user's visited location.
     * @param attraction      The attraction to check against.
     * @return True if the visited location is within proximity, false otherwise.
     */
    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
    }

    /**
     * Retrieves the reward points for visiting an attraction.
     *
     * @param attraction The attraction.
     * @param user       The user visiting the attraction.
     * @return The number of reward points awarded.
     */
    public int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    /**
     * Calculates the distance between two locations using the Haversine formula.
     *
     * @param loc1 The first location.
     * @param loc2 The second location.
     * @return The distance in miles between the two locations.
     */
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
