package com.hzwnrw.jellyfin.repository;

import com.hzwnrw.jellyfin.model.UserExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpirationRepository extends JpaRepository<UserExpiration, String> {
    List<UserExpiration> findByProcessedFalse();
}