package com.openclassrooms.tourguide.tracker;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import gpsUtil.location.VisitedLocation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

@Slf4j
public class Tracker {

    private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final TourGuideService tourGuideService;
    private boolean stop = false;
    private boolean isTrackingComplete = false;
    private final ExecutorService executorService = Executors.newFixedThreadPool(100); // Exemple : 10 threads



    public Tracker(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
    }

    public void stopTracking() {
        stop = true;
		scheduler.shutdown();
        log.info("Tracker stopped");
    }

    public void startTracking() {
        log.info("Tracker start");
        scheduler.scheduleAtFixedRate(() -> {
            StopWatch stopWatch = new StopWatch();
            List<User> users = tourGuideService.getAllUsers();
            log.info("Begin Tracker. Tracking {} users.", users.size());
            stopWatch.start();

            try {
               // tourGuideService.trackAllUserLocations(users);
                List<CompletableFuture<VisitedLocation>> futures = users.stream()
                        .map(user -> tourGuideService.trackUserLocationAsync(user, executorService)
                                .exceptionally(ex -> {
                                    log.error("Failed to track location for user '{}': {}", user.getUserName(), ex.getMessage());
                                    return null;
                                }))
                        .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (Exception ex) {
                log.error("Error while tracking user locations: {}", ex.getMessage());
            }

            stopWatch.stop();
            log.debug("Tracker Time Elapsed: {} seconds.", TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
            stopWatch.reset();
        }, 0, trackingPollingInterval, TimeUnit.SECONDS);
    }

    public boolean isTrackingComplete() {
        return isTrackingComplete;
    }

}
