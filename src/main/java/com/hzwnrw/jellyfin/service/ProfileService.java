package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.dto.PasswordChangeRequest;
import com.hzwnrw.jellyfin.dto.ProfileResponse;
import com.hzwnrw.jellyfin.dto.ProfileUpdateRequest;
import com.hzwnrw.jellyfin.model.AppUser;
import com.hzwnrw.jellyfin.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    public ProfileResponse getProfile(String username) {
        log.info("Fetching profile for user: {}", username);
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return mapToProfileResponse(appUser);
    }

    public ProfileResponse updateProfile(String username, ProfileUpdateRequest updateRequest) {
        log.info("Updating profile for user: {}", username);
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        appUser.setName(updateRequest.getName());
        appUser.setEmail(updateRequest.getEmail());
        appUserRepository.save(appUser);

        // Invalidate user cache
        invalidateUserCache(username);

        log.info("Profile updated successfully for user: {}", username);
        return mapToProfileResponse(appUser);
    }

    public void changePassword(String username, PasswordChangeRequest passwordChangeRequest) {
        log.info("Changing password for user: {}", username);
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Verify current password
        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), appUser.getPassword())) {
            log.warn("Invalid current password attempt for user: {}", username);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Verify passwords match
        if (!passwordChangeRequest.getNewPassword().equals(passwordChangeRequest.getConfirmPassword())) {
            log.warn("Password mismatch for user: {}", username);
            throw new IllegalArgumentException("New passwords do not match");
        }

        // Prevent using same password
        if (passwordChangeRequest.getCurrentPassword().equals(passwordChangeRequest.getNewPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        appUser.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        appUserRepository.save(appUser);

        // Invalidate user cache after password change
        invalidateUserCache(username);

        log.info("Password changed successfully for user: {}", username);
    }

    private void invalidateUserCache(String username) {
        log.debug("Invalidating cache for user: {}", username);
        Cache cache = cacheManager.getCache("appUsers");
        if (cache != null) {
            cache.evict("userByUsername::" + username);
        }
    }

    private ProfileResponse mapToProfileResponse(AppUser appUser) {
        return new ProfileResponse(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getName(),
                appUser.getEmail(),
                appUser.getRoles()
        );
    }
}
