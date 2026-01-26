package com.hzwnrw.jellyfin.scheduler;

import com.hzwnrw.jellyfin.repository.ExpirationRepository;
import com.hzwnrw.jellyfin.service.JellyfinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpirationTask {

    private final ExpirationRepository repository;
    private final JellyfinService jellyfinService;

    @Scheduled(cron = "0 0 0 * * *") // Run every day at midnight UTC
    public void checkExpirations() {
        var pending = repository.findByProcessedFalse();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")); // Always check in UTC

        for (var entry : pending) {
            log.debug("Checking expiration for user: {} | Expiry: {} | Now: {}", 
                entry.getUsername(), entry.getExpiryDate(), now);
            
            if (now.isAfter(entry.getExpiryDate()) || now.isEqual(entry.getExpiryDate())) {
                log.info("User {} has expired. Disabling account.", entry.getUsername());
                jellyfinService.updateDisableStatus(entry.getJellyfinUserId(), true);
                entry.setProcessed(true);
                repository.save(entry);
            }
        }
    }
}