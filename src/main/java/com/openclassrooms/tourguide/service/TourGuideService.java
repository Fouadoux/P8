package com.openclassrooms.tourguide.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;


import java.time.LocalDateTime;
import java.time.ZoneOffset;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

/**
 * Service class responsible for handling user tracking, rewards calculation,
 * and retrieving nearby attractions and trip deals.
 */
@Slf4j
@Service
public class TourGuideService {

    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;
    private final TripPricer tripPricer = new TripPricer();
    boolean testMode = true;
    private ExecutorService executorService;

    /**
     * Constructor initializing the service with required dependencies.
     *
     * @param gpsUtil        The GPS utility service.
     * @param rewardsService The rewards calculation service.
     * @param executorService The executor service for handling concurrent operations.
     */
  public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService, ExecutorService executorService) {
        this.gpsUtil = gpsUtil;
        this.rewardsService = rewardsService;
        this.executorService = executorService;
        Locale.setDefault(Locale.US);

        if (testMode) {
            log.info("TestMode enabled");
            log.debug("Initializing users");
            initializeInternalUsers();
            log.debug("Finished initializing users");
        }
    }

    /**
     * Retrieves a user's reward points.
     *
     * @param user The user whose rewards are being retrieved.
     * @return A list of UserReward objects containing earned rewards.
     */
    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    /**
     * Retrieves the last known location of a user or tracks a new location if none exists.
     *
     * @param user The user whose location is being retrieved.
     * @return The last visited location of the user.
     */
    public VisitedLocation getUserLocation(User user) {
        VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
                : trackUserLocation(user);
        return visitedLocation;
    }

    /**
     * Retrieves a user by username.
     *
     * @param userName The username of the user.
     * @return The User object if found, otherwise null.
     */
    public User getUser(String userName) {
        return internalUserMap.get(userName);
    }

    /**
     * Retrieves all registered users.
     *
     * @return A list of all users.
     */
    public List<User> getAllUsers() {
        return internalUserMap.values().stream().collect(Collectors.toList());
    }

    /**
     * Adds a new user if they are not already registered.
     *
     * @param user The user to be added.
     */
    public void addUser(User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
        }
    }

    /**
     * Retrieves trip deals based on a user's profile and reward points.
     *
     * @param user The user requesting trip deals.
     * @return A list of recommended trip providers.
     */
    public List<Provider> getTripDeals(User user) {
        int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
        List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
                user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
                user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
        user.setTripDeals(providers);
        return providers;
    }

    /**
     * Tracks the user's current location synchronously and updates their visited locations.
     *
     * @param user The user whose location is being tracked.
     * @return The visited location object.
     */
    public VisitedLocation trackUserLocation(User user) {

        VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
        user.addToVisitedLocations(visitedLocation);
        rewardsService.calculateRewards(user);
        return visitedLocation;
    }

    /**
     * Tracks a user's location asynchronously using an executor service.
     *
     * @param user The user whose location is being tracked.
     * @return A CompletableFuture representing the tracking operation.
     */
    public CompletableFuture<VisitedLocation> trackUserLocationAsync(User user, ExecutorService executorService) {

        return CompletableFuture.supplyAsync(() -> {

            try {
                VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
                user.addToVisitedLocations(visitedLocation);
                rewardsService.calculateRewards(user);
                return visitedLocation;
            } catch (Exception e) {
                log.error("Error tracking location for user '{}': {}", user.getUserName(), e.getMessage(), e);
                throw new RuntimeException("Error tracking user location", e);
            }
        }, executorService);
    }

    /**
     * Tracks the locations of all users concurrently.
     *
     * @param users The list of users whose locations need to be tracked.
     */
    public void trackAllUserLocations(List<User> users) {
        try {

            List<CompletableFuture<VisitedLocation>> futures = users.stream()
                    .map(user -> trackUserLocationAsync(user, executorService)
                            .exceptionally(ex -> {
                                log.error("Failed to track location for user '{}': {}", user.getUserName(), ex.getMessage());
                                return null;
                            }))
                    .toList();

            log.info("Waiting for all tracking tasks to complete...");
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("All tracking tasks completed!");

        } catch (Exception e) {
            log.error("Error during tracking: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves the five nearest tourist attractions based on the user's current location.
     *
     * @param user            The user requesting nearby attractions.
     * @param visitedLocation The user's last known location.
     * @return A list of JSON objects representing the nearby attractions.
     */
    public List<ObjectNode> getNearByAttractions(User user, VisitedLocation visitedLocation) {

        List<Attraction> attractions = gpsUtil.getAttractions();
        Map<Attraction, Double> distanceMap = new HashMap<>();

        for (Attraction attraction : attractions) {
            Double dis = rewardsService.getDistance(attraction, visitedLocation.location);
            distanceMap.put(attraction, dis);
        }

        List<Map.Entry<Attraction, Double>> sortedAttractions = distanceMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .toList();

        ObjectMapper objectMapper = new ObjectMapper();
        List<ObjectNode> jsonAttractions = sortedAttractions.stream()
                .map(entry -> {
                    Attraction attraction = entry.getKey();
                    Double distance = entry.getValue();
                    ObjectNode attractionJson = objectMapper.createObjectNode();

                    attractionJson.put("name", attraction.attractionName);
                    attractionJson.put("attractionLatitude", attraction.latitude);
                    attractionJson.put("attractionLongitude", attraction.longitude);
                    attractionJson.put("userLatitude", visitedLocation.location.latitude);
                    attractionJson.put("userLongitude", visitedLocation.location.longitude);
                    attractionJson.put("distance", distance);
                    attractionJson.put("rewardPoints", rewardsService.getRewardPoints(attraction, user));

                    return attractionJson;
                })
                .collect(Collectors.toList());

        return jsonAttractions;

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
        log.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
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
