package com.hzwnrw.jellyfin.controller;

import com.hzwnrw.jellyfin.dto.ProfileResponse;
import com.hzwnrw.jellyfin.service.ProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfilePageControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfilePageController profilePageController;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void profilePageReturnsViewAndModel() throws Exception {
        ProfileResponse profile = new ProfileResponse(1L, "alice", "Alice", "alice@example.com", "ROLE_USER");
        when(profileService.getProfile("alice")).thenReturn(profile);
        Model model = new ExtendedModelMap();

        String viewName = profilePageController.profilePage(model);

        assertEquals("profile", viewName);
        assertEquals(profile, model.getAttribute("profile"));
    }
}
