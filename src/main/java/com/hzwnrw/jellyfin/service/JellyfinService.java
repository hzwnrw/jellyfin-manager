package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.model.JellyfinUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;

@Service
@Slf4j
public class JellyfinService {

    private final RestClient restClient;

    public JellyfinService(@Value("${jellyfin.url}") String baseUrl,
                           @Value("${jellyfin.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Emby-Token", apiKey)
                .build();
    }

    public List<JellyfinUser> getAllUsers() {
        log.info("Fetching all users from Jellyfin");
        // /Users (plural) returns an Array []
        List<JellyfinUser> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/Users")
                        .queryParam("includePolicy", true)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<JellyfinUser>>() {});
        log.info("Retrieved {} users from Jellyfin", users != null ? users.size() : 0);
        return users;
    }

    public void updateDisableStatus(String userId, boolean disable) {
        log.info("Updating disable status for user ID: {} to {}", userId, disable);
        // FIX: Expecting a single Object {}, not a List
        JellyfinUser user = restClient.get()
                .uri("/Users/{id}", userId)
                .retrieve()
                .body(JellyfinUser.class);

        if (user != null && user.getPolicy() != null) {
            // Update the local policy object
            user.getPolicy().setDisabled(disable);

            // Post the updated policy back to Jellyfin
            restClient.post()
                    .uri("/Users/{id}/Policy", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user.getPolicy())
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully updated disable status for user ID: {}", userId);
        } else {
            log.warn("Failed to update disable status: user or policy is null for ID: {}", userId);
        }
    }
}