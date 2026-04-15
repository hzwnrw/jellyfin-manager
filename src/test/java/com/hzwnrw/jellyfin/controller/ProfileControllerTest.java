package com.hzwnrw.jellyfin.controller;

import com.hzwnrw.jellyfin.dto.PasswordChangeRequest;
import com.hzwnrw.jellyfin.dto.ProfileResponse;
import com.hzwnrw.jellyfin.dto.ProfileUpdateRequest;
import com.hzwnrw.jellyfin.service.ProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController).build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getProfileReturnsCurrentUsersProfile() throws Exception {
        when(profileService.getProfile("alice"))
                .thenReturn(new ProfileResponse(1L, "alice", "Alice", "alice@example.com", "ROLE_USER"));

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void updateProfileReturnsUpdatedProfile() throws Exception {
        when(profileService.updateProfile(eq("alice"), any(ProfileUpdateRequest.class)))
                .thenReturn(new ProfileResponse(1L, "alice", "Alice Updated", "updated@example.com", "ROLE_USER"));

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice Updated","email":"updated@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void changePasswordReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/profile/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"old-pass","newPassword":"new-pass","confirmPassword":"new-pass"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(profileService).changePassword(eq("alice"), any(PasswordChangeRequest.class));
    }

    @Test
    void changePasswordReturnsBadRequestWhenServiceRejectsInput() throws Exception {
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(profileService).changePassword(eq("alice"), any(PasswordChangeRequest.class));

        mockMvc.perform(post("/api/profile/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrong","newPassword":"new-pass","confirmPassword":"new-pass"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Current password is incorrect"));
    }
}
