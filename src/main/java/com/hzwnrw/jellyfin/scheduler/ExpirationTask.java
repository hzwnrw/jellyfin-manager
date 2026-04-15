package com.hzwnrw.jellyfin.scheduler;

import com.hzwnrw.jellyfin.repository.ExpirationRepository;
import com.hzwnrw.jellyfin.service.JellyfinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpirationTask {

    private final ExpirationRepository repository;
    private final JellyfinService jellyfinService;

    @Value("${app.timezone:Asia/Kuala_Lumpur}")
    private String appTimezone;

    @Scheduled(cron = "0 0 0 * * *", zone = "${app.timezone:Asia/Kuala_Lumpur}")
    public void checkExpirations() {
        ZoneId appZone = ZoneId.of(appTimezone);
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime nowLocal = nowUtc.withZoneSameInstant(appZone);
        List<com.hzwnrw.jellyfin.model.UserExpiration> pending =
                repository.findByProcessedFalseAndExpiryDateLessThanEqual(nowUtc);

        if (pending.isEmpty()) {
            log.debug("No expirations due at local midnight {} ({})", nowLocal, appTimezone);
            return;
        }

        for (var entry : pending) {
            ZonedDateTime expiryLocal = entry.getExpiryDate().withZoneSameInstant(appZone);
            log.info(
                    "User {} has expired. Disabling account. Expiry UTC: {} | Expiry {}: {} | Checked UTC: {}",
                    entry.getUsername(),
                    entry.getExpiryDate(),
                    appTimezone,
                    expiryLocal,
                    nowUtc
            );
            jellyfinService.updateDisableStatus(entry.getJellyfinUserId(), true);
            entry.setProcessed(true);
            repository.save(entry);
        }
    }
}
