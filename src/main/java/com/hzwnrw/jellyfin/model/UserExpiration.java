package com.hzwnrw.jellyfin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class UserExpiration {
    @Id
    private String jellyfinUserId;
    private String username;
    private LocalDateTime expiryDate;
    private boolean processed = false;
}
