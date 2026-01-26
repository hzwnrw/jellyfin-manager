package com.hzwnrw.jellyfin.scheduler;

import com.hzwnrw.jellyfin.service.JellyfinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JellyfinSyncTask {

    private final JellyfinService jellyfinService;

    /**
     * Periodically sync Jellyfin users to the database.
     * Runs every 10 minutes (600000 ms)
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 5000)
    public void syncJellyfinUsers() {
        log.info("Starting scheduled sync with Jellyfin");
        try {
            jellyfinService.syncAndGetAllUsers();
            log.info("Jellyfin sync completed successfully");
        } catch (Exception e) {
            log.error("Error during Jellyfin sync: {}", e.getMessage(), e);
        }
    }
}
