package com.hzwnrw.jellyfin.controller;

import com.hzwnrw.jellyfin.dto.PasswordChangeRequest;
import com.hzwnrw.jellyfin.dto.ProfileResponse;
import com.hzwnrw.jellyfin.dto.ProfileUpdateRequest;
import com.hzwnrw.jellyfin.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        String username = getCurrentUsername();
        log.info("Fetching profile for user: {}", username);
        ProfileResponse profile = profileService.getProfile(username);
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest updateRequest) {
        String username = getCurrentUsername();
        log.info("Updating profile for user: {}", username);
        ProfileResponse profile = profileService.updateProfile(username, updateRequest);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody PasswordChangeRequest passwordChangeRequest) {
        String username = getCurrentUsername();
        log.info("Changing password for user: {}", username);
        
        try {
            profileService.changePassword(username, passwordChangeRequest);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
