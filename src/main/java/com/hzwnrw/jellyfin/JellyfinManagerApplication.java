package com.hzwnrw.jellyfin;

import com.hzwnrw.jellyfin.service.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class JellyfinManagerApplication {
    private final TokenBlacklistService tokenBlacklistService;

    public JellyfinManagerApplication(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public static void main(String[] args) {
        log.info("Starting Jellyfin Manager Application");
        SpringApplication.run(JellyfinManagerApplication.class, args);
        log.info("Jellyfin Manager Application started successfully");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void testRedisConnection() {
        try {
            // Test Redis connectivity by checking if we can set and get a test key
            String testToken = "test-connectivity-token";
            tokenBlacklistService.blacklistToken(testToken, 10); // 10 seconds TTL
            boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(testToken);
            if (isBlacklisted) {
                log.info("Redis connectivity test successful");
            } else {
                log.warn("Redis connectivity test failed - token not found after setting");
            }
        } catch (Exception e) {
            log.error("Redis connectivity test failed with exception: {}", e.getMessage(), e);
        }
    }
}