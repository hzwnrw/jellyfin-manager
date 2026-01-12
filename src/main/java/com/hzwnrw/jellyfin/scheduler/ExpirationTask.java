package com.hzwnrw.jellyfin.scheduler;

import com.hzwnrw.jellyfin.repository.ExpirationRepository;
import com.hzwnrw.jellyfin.service.JellyfinService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ExpirationTask {

    private final ExpirationRepository repository;
    private final JellyfinService jellyfinService;

    @Scheduled(cron = "0 0 0 * * *") // Run every 5 seconds for testing (change back to cron = "0 0 0 * * *" for production)
    public void checkExpirations() {
        var pending = repository.findByProcessedFalse();
        LocalDateTime now = LocalDateTime.now();

        for (var entry : pending) {
            if (now.isAfter(entry.getExpiryDate()) || now.isEqual(entry.getExpiryDate())) {
                jellyfinService.updateDisableStatus(entry.getJellyfinUserId(), true);
                entry.setProcessed(true);
                repository.save(entry);
            }
        }
    }
}