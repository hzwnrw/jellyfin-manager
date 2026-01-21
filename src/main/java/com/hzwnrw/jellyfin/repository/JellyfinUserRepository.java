package com.hzwnrw.jellyfin.repository;

import com.hzwnrw.jellyfin.model.JellyfinUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JellyfinUserRepository extends JpaRepository<JellyfinUser, String> {
    // Add custom queries if needed
}