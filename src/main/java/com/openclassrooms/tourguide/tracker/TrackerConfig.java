package com.openclassrooms.tourguide.tracker;

import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import gpsUtil.GpsUtil;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class TrackerConfig {
    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;


    @Bean
    public void startTracking() {
        TourGuideService tourGuideService =new TourGuideService(gpsUtil,rewardsService);
        Tracker tracker = new Tracker(tourGuideService);

            tracker.startTracking();  // Démarrer le thread Tracker quand tu le souhaites

    }

}
