package com.hzwnrw.jellyfin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.ZonedDateTime;

@Entity
@Data
@Table(name = "user_expiration")
public class UserExpiration {
    @Id
    @Column(name = "jellyfin_user_id", nullable = false, length = 255)
    private String jellyfinUserId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "expiry_date")
    private ZonedDateTime expiryDate;  // Always stored in UTC

    @Column(name = "processed", nullable = false)
    private boolean processed = false;
}
