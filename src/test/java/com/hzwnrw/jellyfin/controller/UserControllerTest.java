package com.hzwnrw.jellyfin.controller;

import com.hzwnrw.jellyfin.model.JellyfinUser;
import com.hzwnrw.jellyfin.model.UserExpiration;
import com.hzwnrw.jellyfin.repository.ExpirationRepository;
import com.hzwnrw.jellyfin.service.JellyfinService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private JellyfinService jellyfinService;

    @Mock
    private ExpirationRepository repository;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        ReflectionTestUtils.setField(userController, "defaultTimezone", "Asia/Kuala_Lumpur");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void indexReturnsViewWithPaginationAndTrackingModel() throws Exception {
        JellyfinUser user = new JellyfinUser();
        user.setId("user-1");
        user.setName("Alice");
        UserExpiration expiration = new UserExpiration();
        expiration.setJellyfinUserId("user-1");
        expiration.setUsername("Alice");
        expiration.setExpiryDate(ZonedDateTime.of(2026, 4, 15, 17, 0, 0, 0, ZoneId.of("UTC")));

        when(jellyfinService.getAllUsers(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
        when(repository.findAll()).thenReturn(List.of(expiration));

        MvcResult result = mockMvc.perform(get("/")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("users", "tracked", "timezone", "currentDate"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andReturn();

        ModelAndViewAssert.assertModelAttributeAvailable(result.getModelAndView(), "tracked");
        assertEquals("Asia/Kuala_Lumpur", result.getModelAndView().getModel().get("timezone"));
    }

    @Test
    void loginPageReturnsLoginView() {
        assertEquals("login", userController.loginPage());
    }

    @Test
    void setExpiryConvertsConfiguredTimezoneToUtcAndSavesEntry() throws Exception {
        mockMvc.perform(post("/set-expiry")
                        .param("userId", "user-1")
                        .param("username", "alice")
                        .param("date", "2026-04-16T01:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        ArgumentCaptor<UserExpiration> captor = ArgumentCaptor.forClass(UserExpiration.class);
        verify(repository).save(captor.capture());
        UserExpiration saved = captor.getValue();

        assertEquals("user-1", saved.getJellyfinUserId());
        assertEquals("alice", saved.getUsername());
        assertEquals(ZonedDateTime.of(2026, 4, 15, 17, 0, 0, 0, ZoneId.of("UTC")), saved.getExpiryDate());
        assertFalse(saved.isProcessed());
    }

    @Test
    void toggleDisablesUserWithoutDeletingExpiry() throws Exception {
        mockMvc.perform(post("/toggle")
                        .param("userId", "user-1")
                        .param("disable", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(jellyfinService).updateDisableStatus("user-1", true);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void toggleEnablesUserAndDeletesExpiry() throws Exception {
        mockMvc.perform(post("/toggle")
                        .param("userId", "user-1")
                        .param("disable", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(jellyfinService).updateDisableStatus("user-1", false);
        verify(repository).deleteById("user-1");
    }

    @Test
    void syncUsersDelegatesToService() throws Exception {
        mockMvc.perform(post("/sync"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(jellyfinService).syncAndGetAllUsers();
    }

}
