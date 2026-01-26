package com.hzwnrw.jellyfin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.ZonedDateTime;

@Entity
@Data
public class UserExpiration {
    @Id
    private String jellyfinUserId;
    private String username;
    private ZonedDateTime expiryDate;  // Always stored in UTC
    private boolean processed = false;
}
