package com.openclassrooms.tourguide.tracker;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

@Slf4j
public class Tracker {

	private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final ExecutorService executorService;
	private final TourGuideService tourGuideService;
	private boolean stop = false;
	private boolean isTrackingComplete = false;

	int timeout = 0;
	public Tracker(TourGuideService tourGuideService) {
		this.tourGuideService = tourGuideService;
		this.executorService = Executors.newFixedThreadPool(10); // Pool de 10 threads
	}

	/*public void stopTracking() {
		stop = true;
		executorService.shutdown(); // Laisser terminer les tâches en cours
		try {
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) { // Timeout pour les tâches restantes
				executorService.shutdownNow(); // Forcer l'arrêt si elles ne terminent pas
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	 */

	public void stopTracking() {
		stop = true;
		log.info(" "+stop);
		log.info("Stopping tracker...");
		executorService.shutdown();  // Force l'arrêt de toutes les tâches en cours
		log.info("Tracker stopped");
	}


/*	public void startTracking() {
		StopWatch stopWatch = new StopWatch();
		while (!stop) {
			if (Thread.currentThread().isInterrupted()) {
				log.info("Tracker stopping");
				break;
			}

			List<User> users = tourGuideService.getAllUsers();
			log.info("Begin Tracker. Tracking {} users.", users.size());
			stopWatch.start();



			List<CompletableFuture<Void>> futures = users.stream()
					.map(user -> CompletableFuture.runAsync(() -> {
						try {
							tourGuideService.trackUserLocation(user);
						} catch (Exception ex) {
							log.error("Error processing user {}: {}", user.getUserName(), ex.getMessage());
						}
					}, executorService))
					.collect(Collectors.toList());

			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

			log.info(futures.size() + " users processed.");

			stopWatch.stop();
			log.debug("Tracker Time Elapsed: {} seconds.", TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
			stopWatch.reset();

			try {
				log.info("Tracker sleeping");
				TimeUnit.SECONDS.sleep(trackingPollingInterval);

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

 */
public void startTracking() {
	scheduler.scheduleAtFixedRate(() -> {
		StopWatch stopWatch = new StopWatch();
		List<User> users = tourGuideService.getAllUsers();
		log.info("Begin Tracker. Tracking {} users.", users.size());
		stopWatch.start();

		List<CompletableFuture<Void>> futures = users.stream()
				.map(user -> CompletableFuture.runAsync(() -> {
					try {
						tourGuideService.trackUserLocation(user);
					} catch (Exception ex) {
						log.error("Error processing user {}: {}", user.getUserName(), ex.getMessage());
					}
				}, executorService))
				.collect(Collectors.toList());

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		isTrackingComplete = true;

		log.info(futures.size() + " users processed.");
		timeout=futures.size();

		stopWatch.stop();
		log.debug("Tracker Time Elapsed: {} seconds.", TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
		stopWatch.reset();
	}, 0, trackingPollingInterval, TimeUnit.SECONDS);
}
	// Méthode pour vérifier si le suivi est terminé
	public boolean isTrackingComplete() {
		return isTrackingComplete;
	}

}
