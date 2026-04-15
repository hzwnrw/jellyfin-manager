package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.dto.PasswordChangeRequest;
import com.hzwnrw.jellyfin.dto.ProfileResponse;
import com.hzwnrw.jellyfin.dto.ProfileUpdateRequest;
import com.hzwnrw.jellyfin.model.AppUser;
import com.hzwnrw.jellyfin.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getProfileReturnsMappedResponse() {
        AppUser user = buildUser();
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        ProfileResponse result = profileService.getProfile("alice");

        assertEquals(user.getId(), result.getId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void getProfileThrowsWhenUserMissing() {
        when(appUserRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> profileService.getProfile("missing"));
    }

    @Test
    void updateProfileSavesChangesAndInvalidatesCache() {
        AppUser user = buildUser();
        ProfileUpdateRequest request = new ProfileUpdateRequest("Alice Updated", "updated@example.com");
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(cacheManager.getCache("appUsers")).thenReturn(cache);

        ProfileResponse result = profileService.updateProfile("alice", request);

        assertEquals("Alice Updated", result.getName());
        assertEquals("updated@example.com", result.getEmail());
        verify(appUserRepository).save(user);
        verify(cache).evict("userByUsername::alice");
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordDoesNotMatch() {
        AppUser user = buildUser();
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profileService.changePassword("alice",
                        new PasswordChangeRequest("wrong-password", "new-pass", "new-pass")));

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void changePasswordThrowsWhenNewPasswordsDoNotMatch() {
        AppUser user = buildUser();
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profileService.changePassword("alice",
                        new PasswordChangeRequest("current-password", "new-pass", "different")));

        assertEquals("New passwords do not match", exception.getMessage());
    }

    @Test
    void changePasswordThrowsWhenReusingCurrentPassword() {
        AppUser user = buildUser();
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profileService.changePassword("alice",
                        new PasswordChangeRequest("current-password", "current-password", "current-password")));

        assertEquals("New password must be different from current password", exception.getMessage());
    }

    @Test
    void changePasswordEncodesPasswordSavesUserAndInvalidatesCache() {
        AppUser user = buildUser();
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("re-encoded");
        when(cacheManager.getCache("appUsers")).thenReturn(cache);

        profileService.changePassword("alice",
                new PasswordChangeRequest("current-password", "new-pass", "new-pass"));

        assertEquals("re-encoded", user.getPassword());
        verify(appUserRepository).save(user);
        verify(cache).evict("userByUsername::alice");
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("encoded-password");
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setRoles("ROLE_USER");
        return user;
    }
}
