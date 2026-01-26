package com.hzwnrw.jellyfin.repository;

import com.hzwnrw.jellyfin.model.JellyfinUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JellyfinUserRepository extends JpaRepository<JellyfinUser, String> {
    /**
     * Find all users with pagination and sorting support
     */
    Page<JellyfinUser> findAll(Pageable pageable);
}