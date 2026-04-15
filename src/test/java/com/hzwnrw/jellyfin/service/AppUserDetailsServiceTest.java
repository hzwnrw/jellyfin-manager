package com.hzwnrw.jellyfin.service;

import com.hzwnrw.jellyfin.model.AppUser;
import com.hzwnrw.jellyfin.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    @Test
    void loadUserByUsernameBuildsSpringSecurityUser() {
        AppUser appUser = new AppUser();
        appUser.setUsername("admin");
        appUser.setPassword("encoded-password");
        appUser.setRoles("ROLE_ADMIN,ROLE_USER");
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(appUser));

        UserDetails result = appUserDetailsService.loadUserByUsername("admin");

        assertEquals("admin", result.getUsername());
        assertEquals("encoded-password", result.getPassword());
        assertEquals(2, result.getAuthorities().size());
    }

    @Test
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(appUserRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> appUserDetailsService.loadUserByUsername("missing"));
    }
}
