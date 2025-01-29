package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Slf4j
@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    private ExecutorService executorService = Executors.newFixedThreadPool(100); // Exemple : 10 threads


    // proximity in miles
    private int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    public void calculateRewards(User user) {
       // log.info("Executing calculateReward for user {} in thread: {}", user.getUserName(), Thread.currentThread().getName());

        List<VisitedLocation> userLocations = user.getVisitedLocations(); //liste des localisations visité
        List<Attraction> attractions = gpsUtil.getAttractions(); // liste des localisations des attractions

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

	public CompletableFuture<Void> calculateRewardsAsync(User user, ExecutorService executorService, List<Attraction> attractions) {
		List<VisitedLocation> userLocations = user.getVisitedLocations(); // Liste des localisations visitées


		return CompletableFuture.runAsync(() -> {
			log.info("Starting calculateRewards for user '{}' in thread: {}", user.getUserName(), Thread.currentThread().getName());

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
					log.info("Successfully tracked reward for user '{}'", user.getUserName());
				}
			} catch (Exception e) {
				log.error("Error while calculating rewards asynchronously for user '{}': {}", user.getUserName(), e.getMessage(), e);
				throw new RuntimeException("Error while calculating rewards asynchronously", e);
			}
		}, executorService);
	}

    public void trackCalculateRewardsAsync(List<User> users) {
        List<Attraction> attractions = gpsUtil.getAttractions();

        try {
            List<CompletableFuture<Void>> futures = users.stream()
                    .map(user -> calculateRewardsAsync(user, executorService, attractions))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executorService.shutdown();
        }
    }

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
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
