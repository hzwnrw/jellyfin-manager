package com.hzwnrw.jellyfin.security;

import com.hzwnrw.jellyfin.model.AppUser;
import com.hzwnrw.jellyfin.repository.AppUserRepository;
import com.hzwnrw.jellyfin.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AppUserRepository appUserRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.cookie-secure:false}")
    private boolean secureCookie;

    @Value("${app.security.cookie-same-site:Lax}")
    private String sameSite;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            log.error("Google OAuth2 user has no email attribute");
            response.sendRedirect("/login?error=no_email");
            return;
        }

        AppUser appUser = findOrCreateUser(email, name);
        String jwt = jwtUtils.generateToken(appUser.getUsername());

        ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", jwt)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/")
                .maxAge(jwtUtils.getExpirationTime() / 1000L)
                .build();
        response.addHeader("Set-Cookie", jwtCookie.toString());

        log.info("Google OAuth2 login successful for: {}", email);
        response.sendRedirect("/");
    }

    private AppUser findOrCreateUser(String email, String name) {
        Optional<AppUser> existing = appUserRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }

        String username = email.substring(0, email.indexOf('@'));
        AppUser newUser = new AppUser();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setName(name != null ? name : username);
        newUser.setEmail(email);
        newUser.setRoles("USER");

        appUserRepository.save(newUser);
        log.info("Created new app user from Google OAuth2: {} ({})", username, email);
        return newUser;
    }
}
