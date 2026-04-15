package com.hzwnrw.jellyfin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void testRedisConnectionWritesAndDeletesProbeKeyWhenRedisIsHealthy() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey("test_connection")).thenReturn(true);

        tokenBlacklistService.testRedisConnection();

        verify(valueOperations).set("test_connection", "ok");
        verify(redisTemplate).delete("test_connection");
    }

    @Test
    void blacklistTokenStoresTokenWithTtlWhenVerificationSucceeds() {
        String token = "abcdefghijklmnopqrstuvwxyz";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey("blacklist_token:" + token)).thenReturn(true);

        tokenBlacklistService.blacklistToken(token, 120L);

        verify(valueOperations).set("blacklist_token:" + token, "blacklisted", Duration.ofSeconds(120L));
    }

    @Test
    void blacklistTokenThrowsWhenVerificationFails() {
        String token = "abcdefghijklmnopqrstuvwxyz";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey("blacklist_token:" + token)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> tokenBlacklistService.blacklistToken(token, 120L));
    }

    @Test
    void isTokenBlacklistedReturnsFalseForNullOrEmptyTokens() {
        assertFalse(tokenBlacklistService.isTokenBlacklisted(null));
        assertFalse(tokenBlacklistService.isTokenBlacklisted(""));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void isTokenBlacklistedReturnsTrueWhenKeyExists() {
        String token = "abcdefghijklmnopqrstuvwxyz";
        when(redisTemplate.hasKey("blacklist_token:" + token)).thenReturn(true);

        boolean result = tokenBlacklistService.isTokenBlacklisted(token);

        assertTrue(result);
    }

    @Test
    void isTokenBlacklistedFailsClosedWhenRedisErrors() {
        String token = "abcdefghijklmnopqrstuvwxyz";
        when(redisTemplate.hasKey("blacklist_token:" + token)).thenThrow(new RuntimeException("redis down"));

        boolean result = tokenBlacklistService.isTokenBlacklisted(token);

        assertTrue(result);
    }
}
