package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.model.AppUser;
import com.hzwnrw.jellyfin.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserCacheServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private AppUserCacheService appUserCacheService;

    @Test
    void getUserByUsernameDelegatesToRepository() {
        AppUser user = new AppUser();
        user.setUsername("alice");
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        Optional<AppUser> result = appUserCacheService.getUserByUsername("alice");

        assertEquals(Optional.of(user), result);
    }

    @Test
    void getUserByIdDelegatesToRepository() {
        AppUser user = new AppUser();
        user.setId(7L);
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));

        Optional<AppUser> result = appUserCacheService.getUserById(7L);

        assertEquals(Optional.of(user), result);
    }

    @Test
    void invalidateUserCacheEvictsSpecificKeyWhenCacheExists() {
        when(cacheManager.getCache("appUsers")).thenReturn(cache);

        appUserCacheService.invalidateUserCache("alice");

        verify(cache).evict("userByUsername::alice");
    }

    @Test
    void invalidateUserCacheDoesNothingWhenCacheMissing() {
        when(cacheManager.getCache("appUsers")).thenReturn(null);

        appUserCacheService.invalidateUserCache("alice");

        verify(cacheManager).getCache("appUsers");
        verifyNoInteractions(cache);
    }

    @Test
    void invalidateAllUserCacheClearsCacheWhenPresent() {
        when(cacheManager.getCache("appUsers")).thenReturn(cache);

        appUserCacheService.invalidateAllUserCache();

        verify(cache).clear();
    }

    @Test
    void invalidateAllUserCacheDoesNothingWhenCacheMissing() {
        when(cacheManager.getCache("appUsers")).thenReturn(null);

        appUserCacheService.invalidateAllUserCache();

        verify(cacheManager).getCache("appUsers");
        verifyNoInteractions(cache);
    }
}
