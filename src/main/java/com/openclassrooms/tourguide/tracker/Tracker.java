package com.openclassrooms.tourguide.tracker;

import java.util.List;
import java.util.concurrent.*;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import org.springframework.stereotype.Component;

/**
 * The {@code Tracker} class is responsible for periodically tracking user locations.
 * It schedules a background task that retrieves all users and updates their locations at a fixed interval.
 * This ensures that user positions are kept up to date for accurate recommendations and rewards calculations.
 *
 * <p>The tracking runs asynchronously using a {@link ScheduledExecutorService} to avoid blocking the main application flow.</p>
 *
 * <h2>Usage</h2>
 * <p>The tracker automatically starts when the application initializes, thanks to the {@link PostConstruct} annotation.</p>
 * <p>To stop tracking, call {@link #stopTracking()}, which will gracefully shut down the scheduled task.</p>
 *
 */
@Slf4j
@Component
public class Tracker {

    /** Interval in seconds between tracking executions. */
    private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(1);

    /** Scheduled executor service for periodic tracking. */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /** The service responsible for managing user locations. */
    private final TourGuideService tourGuideService;

    /** Flag to indicate whether tracking should stop. */
    private boolean stop = false;


    /**
     * Constructs a {@code Tracker} with the specified {@code TourGuideService}.
     *
     * @param tourGuideService the service responsible for tracking user locations
     */
    public Tracker(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
    }

    /**
     * Stops the tracking process and shuts down the scheduler.
     */
    public void stopTracking() {
        stop = true;
        scheduler.shutdown();
        log.info("Tracker stopped");
    }

    /**
     * Starts tracking user locations at a fixed interval.
     * <p>
     * The tracker retrieves all registered users and asynchronously updates their locations.
     * It logs execution time to monitor performance.
     * </p>
     *
     * <p>This method is automatically called when the application starts.</p>
     */
    @PostConstruct
    public void startTracking() {
        log.info("Tracker start");
        scheduler.scheduleAtFixedRate(() -> {
            StopWatch stopWatch = new StopWatch();
            List<User> users = tourGuideService.getAllUsers();
            log.info("Begin Tracker. Tracking {} users.", users.size());
            stopWatch.start();

            try {
                tourGuideService.trackAllUserLocations(users);
            } catch (Exception ex) {
                log.error("Error while tracking user locations: {}", ex.getMessage());
            }

            stopWatch.stop();
            log.debug("Tracker Time Elapsed: {} seconds.", TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
            stopWatch.reset();
        }, 0, trackingPollingInterval, TimeUnit.SECONDS);
    }

}
