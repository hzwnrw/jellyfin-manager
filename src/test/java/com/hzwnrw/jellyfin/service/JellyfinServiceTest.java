package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.model.JellyfinUser;
import com.hzwnrw.jellyfin.model.UserPolicy;
import com.hzwnrw.jellyfin.repository.JellyfinUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JellyfinServiceTest {

    private interface RequestHeadersSpecStub extends RestClient.RequestHeadersSpec<RequestHeadersSpecStub> {
    }

    private interface RequestHeadersUriSpecStub extends RestClient.RequestHeadersUriSpec<RequestHeadersSpecStub> {
    }

    @Mock
    private JellyfinUserRepository jellyfinUserRepository;

    @Mock
    private JellyfinUserCacheService jellyfinUserCacheService;

    @Mock
    private RestClient restClient;

    @Mock
    private RequestHeadersUriSpecStub requestHeadersUriSpec;

    @Mock
    private RequestHeadersSpecStub requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Test
    void syncAndGetAllUsersSavesFetchedUsersAndReturnsDatabaseState() {
        JellyfinService jellyfinService = buildService();
        JellyfinUser fetchedUser = new JellyfinUser();
        fetchedUser.setId("user-1");
        fetchedUser.setName("Alice");

        doReturn(requestHeadersUriSpec).when(restClient).get();
        when(requestHeadersUriSpec.uri(org.mockito.ArgumentMatchers.<Function<UriBuilder, URI>>any()))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<JellyfinUser>>>any()))
                .thenReturn(List.of(fetchedUser));
        when(jellyfinUserRepository.findAll()).thenReturn(List.of(fetchedUser));

        List<JellyfinUser> result = jellyfinService.syncAndGetAllUsers();

        verify(jellyfinUserRepository).saveAll(List.of(fetchedUser));
        verify(jellyfinUserRepository).findAll();
        assertEquals(List.of(fetchedUser), result);
    }

    @Test
    void syncAndGetAllUsersStillReturnsRepositoryDataWhenApiFails() {
        JellyfinService jellyfinService = buildService();
        JellyfinUser existingUser = new JellyfinUser();
        existingUser.setId("user-1");

        doReturn(requestHeadersUriSpec).when(restClient).get();
        when(requestHeadersUriSpec.uri(org.mockito.ArgumentMatchers.<Function<UriBuilder, URI>>any()))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<JellyfinUser>>>any()))
                .thenThrow(new RuntimeException("api failure"));
        when(jellyfinUserRepository.findAll()).thenReturn(List.of(existingUser));

        List<JellyfinUser> result = jellyfinService.syncAndGetAllUsers();

        verify(jellyfinUserRepository, never()).saveAll(anyList());
        assertEquals(List.of(existingUser), result);
    }

    @Test
    void getAllUsersReturnsRepositoryResults() {
        JellyfinService jellyfinService = buildService();
        JellyfinUser user = new JellyfinUser();
        user.setId("user-1");
        when(jellyfinUserRepository.findAll()).thenReturn(List.of(user));

        List<JellyfinUser> result = jellyfinService.getAllUsers();

        assertEquals(List.of(user), result);
    }

    @Test
    void getAllUsersPageableDelegatesToRepository() {
        JellyfinService jellyfinService = buildService();
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<JellyfinUser> page = new PageImpl<>(List.of(new JellyfinUser()), pageRequest, 1);
        when(jellyfinUserRepository.findAll(pageRequest)).thenReturn(page);

        Page<JellyfinUser> result = jellyfinService.getAllUsers(pageRequest);

        assertEquals(page, result);
    }

    @Test
    void updateDisableStatusPostsUpdatedPolicySavesUserAndInvalidatesCache() {
        JellyfinService jellyfinService = buildService();
        JellyfinUser user = new JellyfinUser();
        user.setId("user-1");
        user.setPolicy(new UserPolicy());

        doReturn(requestHeadersUriSpec).when(restClient).get();
        when(requestHeadersUriSpec.uri("/Users/{id}", "user-1")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JellyfinUser.class)).thenReturn(user);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/Users/{id}/Policy", "user-1")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UserPolicy.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        jellyfinService.updateDisableStatus("user-1", true);

        assertEquals(true, user.getPolicy().isDisabled());
        verify(jellyfinUserRepository).save(user);
        verify(jellyfinUserCacheService).invalidateUserCache("user-1");
        verify(jellyfinUserCacheService).invalidateAllUsersCache();
    }

    @Test
    void updateDisableStatusDoesNothingWhenFetchedUserHasNoPolicy() {
        JellyfinService jellyfinService = buildService();
        JellyfinUser user = new JellyfinUser();
        user.setId("user-1");
        user.setPolicy(null);

        doReturn(requestHeadersUriSpec).when(restClient).get();
        when(requestHeadersUriSpec.uri("/Users/{id}", "user-1")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JellyfinUser.class)).thenReturn(user);

        jellyfinService.updateDisableStatus("user-1", true);

        verify(jellyfinUserRepository, never()).save(any());
        verifyNoInteractions(jellyfinUserCacheService);
        verify(restClient, never()).post();
    }

    @Test
    void updateDisableStatusSwallowsRemoteErrors() {
        JellyfinService jellyfinService = buildService();

        doReturn(requestHeadersUriSpec).when(restClient).get();
        when(requestHeadersUriSpec.uri("/Users/{id}", "user-1")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JellyfinUser.class)).thenThrow(new RuntimeException("remote failure"));

        jellyfinService.updateDisableStatus("user-1", true);

        verify(jellyfinUserRepository, never()).save(any());
    }

    private JellyfinService buildService() {
        JellyfinService jellyfinService = new JellyfinService(
                "http://localhost:8096",
                "api-key",
                jellyfinUserRepository,
                jellyfinUserCacheService
        );
        ReflectionTestUtils.setField(jellyfinService, "restClient", restClient);
        return jellyfinService;
    }
}
