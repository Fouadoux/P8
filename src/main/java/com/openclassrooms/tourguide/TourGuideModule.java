package com.openclassrooms.tourguide;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.service.RewardsService;

import java.util.concurrent.*;

/**
 * The {@code TourGuideModule} class is a Spring configuration class responsible for defining
 * and providing beans required for the TourGuide application.
 *
 * <p>This module initializes essential dependencies such as:
 * <ul>
 *   <li>{@link GpsUtil} - A utility service to retrieve GPS locations.</li>
 *   <li>{@link RewardCentral} - A service that provides attraction reward points.</li>
 *   <li>{@link RewardsService} - A service responsible for calculating user rewards.</li>
 * </ul>
 * </p>
 *
 * <p>The class also injects an {@link ExecutorService} to handle asynchronous processing.</p>
 *
 */
@Configuration
public class TourGuideModule {

	/** Executor service for handling asynchronous tasks. */
	private final ExecutorService executorService;

	/**
	 * Constructs a {@code TourGuideModule} with the specified {@link ExecutorService}.
	 *
	 * @param executorService the executor service for handling concurrent operations
	 */
	public TourGuideModule(ExecutorService executorService) {
		this.executorService = executorService;
	}

	/**
	 * Provides a singleton instance of {@link GpsUtil}.
	 *
	 * @return a new instance of {@link GpsUtil}
	 */
	@Bean
	public GpsUtil getGpsUtil() {
		return new GpsUtil();
	}
	
	@Bean
	public RewardsService getRewardsService() {
		return new RewardsService(getGpsUtil(), getRewardCentral(),executorService);
	}
	
	@Bean
	public RewardCentral getRewardCentral() {
		return new RewardCentral();
	}



}
