package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.model.AppUser;
import com.hzwnrw.jellyfin.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppUserCacheService {

    private final AppUserRepository appUserRepository;
    private final CacheManager cacheManager;

    private static final String CACHE_NAME = "appUsers";
    private static final String USER_BY_USERNAME_CACHE = "userByUsername";

    /**
     * Get user by username with caching
     */
    public Optional<AppUser> getUserByUsername(String username) {
        log.debug("Fetching user: {} from cache or database", username);
        return appUserRepository.findByUsername(username);
    }

    /**
     * Invalidate user cache when profile is updated
     */
    public void invalidateUserCache(String username) {
        log.info("Invalidating cache for user: {}", username);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(USER_BY_USERNAME_CACHE + "::" + username);
        }
    }

    /**
     * Invalidate all user cache
     */
    public void invalidateAllUserCache() {
        log.info("Invalidating all user cache");
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Get user by ID with caching
     */
    public Optional<AppUser> getUserById(Long id) {
        log.debug("Fetching user with ID: {}", id);
        return appUserRepository.findById(id);
    }
}
