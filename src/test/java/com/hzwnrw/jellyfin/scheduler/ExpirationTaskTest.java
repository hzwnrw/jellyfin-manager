package com.hzwnrw.jellyfin.scheduler;

import com.hzwnrw.jellyfin.model.UserExpiration;
import com.hzwnrw.jellyfin.repository.ExpirationRepository;
import com.hzwnrw.jellyfin.service.JellyfinService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpirationTaskTest {

    @Mock
    private ExpirationRepository repository;

    @Mock
    private JellyfinService jellyfinService;

    @InjectMocks
    private ExpirationTask expirationTask;

    @Test
    void checkExpirationsDoesNothingWhenNoPendingEntriesExist() {
        ReflectionTestUtils.setField(expirationTask, "appTimezone", "Asia/Kuala_Lumpur");
        when(repository.findByProcessedFalseAndExpiryDateLessThanEqual(any(ZonedDateTime.class)))
                .thenReturn(List.of());

        expirationTask.checkExpirations();

        verify(repository).findByProcessedFalseAndExpiryDateLessThanEqual(any(ZonedDateTime.class));
        verifyNoInteractions(jellyfinService);
        verify(repository, never()).save(any(UserExpiration.class));
    }

    @Test
    void checkExpirationsDisablesAndMarksEachDueEntryProcessed() {
        ReflectionTestUtils.setField(expirationTask, "appTimezone", "Asia/Kuala_Lumpur");

        UserExpiration first = buildExpiration("user-1", "alice");
        UserExpiration second = buildExpiration("user-2", "bob");

        when(repository.findByProcessedFalseAndExpiryDateLessThanEqual(any(ZonedDateTime.class)))
                .thenReturn(List.of(first, second));
        when(repository.save(any(UserExpiration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expirationTask.checkExpirations();

        verify(jellyfinService).updateDisableStatus("user-1", true);
        verify(jellyfinService).updateDisableStatus("user-2", true);

        ArgumentCaptor<UserExpiration> savedEntries = ArgumentCaptor.forClass(UserExpiration.class);
        verify(repository, times(2)).save(savedEntries.capture());

        assertEquals(List.of("user-1", "user-2"),
                savedEntries.getAllValues().stream().map(UserExpiration::getJellyfinUserId).toList());
        assertTrue(first.isProcessed());
        assertTrue(second.isProcessed());
    }

    private UserExpiration buildExpiration(String userId, String username) {
        UserExpiration expiration = new UserExpiration();
        expiration.setJellyfinUserId(userId);
        expiration.setUsername(username);
        expiration.setExpiryDate(ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(5));
        expiration.setProcessed(false);
        return expiration;
    }
}
