package com.openclassrooms.tourguide.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

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

    private final ExecutorService executorService = Executors.newFixedThreadPool(100); // Exemple : 10 threads

    public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
        this.gpsUtil = gpsUtil;
        this.rewardsService = rewardsService;
        Locale.setDefault(Locale.US);

        if (testMode) {
            logger.info("TestMode enabled");
            logger.debug("Initializing users");
            initializeInternalUsers();
            logger.debug("Finished initializing users");
        }
        tracker = new Tracker(this);
        // Démarrer le thread Tracker
       //  tracker.startTracking();
        addShutDownHook();
    }


    public void startTracking() {
        if (tracker != null) {
            tracker.startTracking();  // Démarrer le thread Tracker quand tu le souhaites
        }
    }

    // Méthode pour arrêter le suivi
    public void stopTracking() {
        if (tracker != null) {
            tracker.stopTracking();  // Arrêter le suivi quand nécessaire
        }
    }

    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    public VisitedLocation getUserLocation(User user) {
        VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
                : trackUserLocation(user);
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

    public VisitedLocation trackUserLocation(User user) {

        VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
        user.addToVisitedLocations(visitedLocation);
        rewardsService.calculateRewards(user);
        return visitedLocation;
    }

    public CompletableFuture<VisitedLocation> trackUserLocationAsync(User user, ExecutorService executorService) {

        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting trackUserLocation for user '{}' in thread: {}", user.getUserName(), Thread.currentThread().getName());

            try {
                // Obtenir la localisation de l'utilisateur
                logger.info("1");
                VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());

                // Ajouter la localisation visitée à l'
                logger.info("2");
                user.addToVisitedLocations(visitedLocation);

                // Calculer les récompenses pour l'utilisateur
                logger.info("3");
                rewardsService.calculateRewards(user);


                logger.info("Successfully tracked location for user '{}'", user.getUserName());
                return visitedLocation;
            } catch (Exception e) {
                logger.error("Error tracking location for user '{}': {}", user.getUserName(), e.getMessage(), e);
                throw new RuntimeException("Error tracking user location", e);
            }
        }, executorService); // Utilisation de l'executor personnalisé
    }

    public void trackAllUserLocations1(List<User> users) {
        try {
            List<CompletableFuture<VisitedLocation>> futures = users.stream()
                    .map(user -> trackUserLocationAsync(user, executorService)
                            .exceptionally(ex -> {
                                logger.error("Failed to track location for user '{}': {}", user.getUserName(), ex.getMessage());
                                return null;
                            }))
                    .toList();

            // Attendre que toutes les tâches soient terminées
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executorService.shutdown();
        }
    }

    public void trackAllUserLocations(List<User> users) {
        try {
            logger.info("Tracking {} users asynchronously...", users.size());

            List<CompletableFuture<VisitedLocation>> futures = users.stream()
                    .map(user -> trackUserLocationAsync(user, executorService)
                            .exceptionally(ex -> {
                                logger.error("Failed to track location for user '{}': {}", user.getUserName(), ex.getMessage());
                                return null;
                            }))
                    .toList();

            logger.info("Waiting for all tracking tasks to complete...");
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            logger.info("All tracking tasks completed!");

        } catch (Exception e) {
            logger.error("Error during tracking: {}", e.getMessage(), e);
        } finally {
            logger.info("Shutting down executorService...");
            executorService.shutdown();
        }
    }

    public List<ObjectNode> getNearByAttractions(User user, VisitedLocation visitedLocation) {

        // Récupérer toutes les attractions et calculer les distances
        List<Attraction> attractions = gpsUtil.getAttractions();
        Map<Attraction, Double> distanceMap = new HashMap<>();

        //Calcule la distance entre la localisation du User
        for (Attraction attraction : attractions) {
            Double dis = rewardsService.getDistance(attraction, visitedLocation.location);
            distanceMap.put(attraction, dis);
        }

        //trier les attractions par distance croissante
        List<Map.Entry<Attraction, Double>> sortedAttractions = distanceMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5) // Limiter aux 5 attractions les plus proches
                .toList();

        // Construire la liste JSON
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

    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                tracker.stopTracking();
            }
        });
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
