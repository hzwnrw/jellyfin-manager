package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.model.JellyfinUser;
import com.hzwnrw.jellyfin.repository.JellyfinUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;

@Service
@Slf4j
public class JellyfinService {

    private final RestClient restClient;
    private final JellyfinUserRepository jellyfinUserRepository;
    private final JellyfinUserCacheService jellyfinUserCacheService;

    public JellyfinService(@Value("${jellyfin.url}") String baseUrl,
                           @Value("${jellyfin.api-key}") String apiKey,
                           JellyfinUserRepository jellyfinUserRepository,
                           JellyfinUserCacheService jellyfinUserCacheService) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Emby-Token", apiKey)
                .build();
        this.jellyfinUserRepository = jellyfinUserRepository;
        this.jellyfinUserCacheService = jellyfinUserCacheService;
    }

    /**
     * Fetches all users from Jellyfin and saves/updates them in the database.
     * Returns the saved users from the database.
     */
    @CacheEvict(value = "jellyfinUsers", allEntries = true)
    public List<JellyfinUser> syncAndGetAllUsers() {
        log.info("Syncing users from Jellyfin API");
        try {
            List<JellyfinUser> users = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/Users")
                            .queryParam("includePolicy", true)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<JellyfinUser>>() {});
            
            if (users != null && !users.isEmpty()) {
                jellyfinUserRepository.saveAll(users);
                log.info("Successfully synced {} users to database", users.size());
            } else {
                log.warn("No users returned from Jellyfin API");
            }
        } catch (Exception e) {
            log.error("Error syncing users from Jellyfin: {}", e.getMessage());
        }
        
        return jellyfinUserRepository.findAll();
    }

    /**
     * Returns all users from the local database (read-through cache).
     */
    @Cacheable(value = "jellyfinUsers", key = "'allUsers'")
    public List<JellyfinUser> getAllUsers() {
        log.debug("Retrieving all users from database");
        return jellyfinUserRepository.findAll();
    }

    /**
     * Returns paginated and sorted users from the local database.
     */
    //@Cacheable(value = "jellyfinUsers", key = "'page::' + #pageable.pageNumber + '::' + #pageable.pageSize + '::' + #pageable.sort.toString()")
    public Page<JellyfinUser> getAllUsers(Pageable pageable) {
        log.debug("Retrieving paginated users from database with sort: {}", pageable.getSort());
        return jellyfinUserRepository.findAll(pageable);
    }

    public void updateDisableStatus(String userId, boolean disable) {
        log.info("Updating disable status for user ID: {} to {}", userId, disable);
        try {
            // Fetch from Jellyfin
            JellyfinUser user = restClient.get()
                    .uri("/Users/{id}", userId)
                    .retrieve()
                    .body(JellyfinUser.class);

            if (user != null && user.getPolicy() != null) {
                // Update the policy object
                user.getPolicy().setDisabled(disable);

                // Post the updated policy back to Jellyfin
                restClient.post()
                        .uri("/Users/{id}/Policy", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(user.getPolicy())
                        .retrieve()
                        .toBodilessEntity();

                // Sync to database
                jellyfinUserRepository.save(user);
                
                // Invalidate user cache
                jellyfinUserCacheService.invalidateUserCache(userId);
                jellyfinUserCacheService.invalidateAllUsersCache();
                
                log.info("Successfully updated disable status for user ID: {} and synced to database", userId);
            } else {
                log.warn("Failed to update disable status: user or policy is null for ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("Error updating disable status for user ID: {}: {}", userId, e.getMessage());
        }
    }
}