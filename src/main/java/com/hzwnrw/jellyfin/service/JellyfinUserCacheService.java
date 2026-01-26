package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.model.JellyfinUser;
import com.hzwnrw.jellyfin.repository.JellyfinUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JellyfinUserCacheService {

    private final JellyfinUserRepository jellyfinUserRepository;
    private final CacheManager cacheManager;

    private static final String CACHE_NAME = "jellyfinUsers";

    /**
     * Get user by ID with caching
     */
    @Cacheable(value = "jellyfinUsers", key = "'userId::' + #userId")
    public Optional<JellyfinUser> getUserById(String userId) {
        log.debug("Fetching Jellyfin user with ID: {}", userId);
        return jellyfinUserRepository.findById(userId);
    }

    /**
     * Get all users with caching
     */
    @Cacheable(value = "jellyfinUsers", key = "'allUsers'")
    public List<JellyfinUser> getAllUsers() {
        log.debug("Fetching all Jellyfin users");
        return jellyfinUserRepository.findAll();
    }

    /**
     * Get paginated users (less likely to be cached, but available)
     */
    public Page<JellyfinUser> getPaginatedUsers(Pageable pageable) {
        log.debug("Fetching paginated Jellyfin users");
        return jellyfinUserRepository.findAll(pageable);
    }

    /**
     * Invalidate cache for a specific user
     */
    public void invalidateUserCache(String userId) {
        log.info("Invalidating cache for Jellyfin user: {}", userId);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict("userId::" + userId);
        }
    }

    /**
     * Invalidate cache for all users
     */
    public void invalidateAllUsersCache() {
        log.info("Invalidating all Jellyfin users cache");
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict("allUsers");
            cache.evict("allUsersWithPolicy");
        }
    }

    /**
     * Invalidate all Jellyfin user cache
     */
    public void clearAllJellyfinUserCache() {
        log.info("Clearing all Jellyfin user cache");
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }
}
