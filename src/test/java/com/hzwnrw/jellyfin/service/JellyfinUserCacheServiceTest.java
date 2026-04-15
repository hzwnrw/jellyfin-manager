package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.model.JellyfinUser;
import com.hzwnrw.jellyfin.repository.JellyfinUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JellyfinUserCacheServiceTest {

    @Mock
    private JellyfinUserRepository jellyfinUserRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private JellyfinUserCacheService jellyfinUserCacheService;

    @Test
    void getUserByIdDelegatesToRepository() {
        JellyfinUser user = new JellyfinUser();
        user.setId("user-1");
        when(jellyfinUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        Optional<JellyfinUser> result = jellyfinUserCacheService.getUserById("user-1");

        assertEquals(Optional.of(user), result);
    }

    @Test
    void getAllUsersDelegatesToRepository() {
        JellyfinUser user = new JellyfinUser();
        user.setId("user-1");
        when(jellyfinUserRepository.findAll()).thenReturn(List.of(user));

        List<JellyfinUser> result = jellyfinUserCacheService.getAllUsers();

        assertEquals(List.of(user), result);
    }

    @Test
    void getPaginatedUsersDelegatesToRepository() {
        PageRequest pageRequest = PageRequest.of(0, 5);
        Page<JellyfinUser> page = new PageImpl<>(List.of(new JellyfinUser()), pageRequest, 1);
        when(jellyfinUserRepository.findAll(pageRequest)).thenReturn(page);

        Page<JellyfinUser> result = jellyfinUserCacheService.getPaginatedUsers(pageRequest);

        assertEquals(page, result);
    }

    @Test
    void invalidateUserCacheEvictsSpecificUserWhenCacheExists() {
        when(cacheManager.getCache("jellyfinUsers")).thenReturn(cache);

        jellyfinUserCacheService.invalidateUserCache("user-1");

        verify(cache).evict("userId::user-1");
    }

    @Test
    void invalidateAllUsersCacheEvictsKnownAggregateKeys() {
        when(cacheManager.getCache("jellyfinUsers")).thenReturn(cache);

        jellyfinUserCacheService.invalidateAllUsersCache();

        verify(cache).evict("allUsers");
        verify(cache).evict("allUsersWithPolicy");
    }

    @Test
    void clearAllJellyfinUserCacheClearsCacheWhenPresent() {
        when(cacheManager.getCache("jellyfinUsers")).thenReturn(cache);

        jellyfinUserCacheService.clearAllJellyfinUserCache();

        verify(cache).clear();
    }
}
