package com.hzwnrw.jellyfin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist_token:";

    @PostConstruct
    public void testRedisConnection() {
        try {
            redisTemplate.opsForValue().set("test_connection", "ok");
            Boolean exists = redisTemplate.hasKey("test_connection");
            if (exists != null && exists) {
                log.info("Redis connection test successful");
                redisTemplate.delete("test_connection");
            } else {
                log.error("Redis connection test failed: set succeeded but hasKey failed");
            }
        } catch (Exception e) {
            log.error("Redis connection test failed: {}", e.getMessage(), e);
        }
    }

    public void blacklistToken(String token, long ttlInSeconds) {
        try {
            String redisKey = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(redisKey, "blacklisted", Duration.ofSeconds(ttlInSeconds));
            // Verify the token was set
            Boolean exists = redisTemplate.hasKey(redisKey);
            if (exists != null && exists) {
                log.info("Token blacklisted successfully in Redis with TTL {} seconds: {}", ttlInSeconds, token.substring(0, 20) + "...");
            } else {
                log.error("Token set failed, key not found in Redis after set: {}", token.substring(0, 20) + "...");
                throw new RuntimeException("Redis set operation failed verification");
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis: {} - Error: {}", token.substring(0, 20) + "...", e.getMessage(), e);
            throw e; // Re-throw so caller knows it failed
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return false;
            }
            String redisKey = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(redisKey);
            boolean blacklisted = exists != null && exists;
            if (blacklisted) {
                log.warn("Access denied: Token is blacklisted: {}", token.substring(0, 20) + "...");
            }
            return blacklisted;
        } catch (Exception e) {
            log.error("CRITICAL: Failed to check if token is blacklisted in Redis - denying access for security: {} - Error: {}", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null", e.getMessage(), e);
            return true; // Deny access on Redis error for security
        }
    }
}