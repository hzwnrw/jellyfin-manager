package com.hzwnrw.jellyfin.controller;

import com.hzwnrw.jellyfin.dto.LoginRequest;
import com.hzwnrw.jellyfin.utils.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // Generates constructor for final fields
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Value("${app.security.cookie-secure:false}")
    private boolean secureCookie;

    @Value("${app.security.cookie-same-site:Lax}")
    private String sameSite;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateToken(authentication.getName());
            ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", jwt)
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite(sameSite)
                    .path("/")
                    .maxAge(jwtUtils.getExpirationTime() / 1000L)
                    .build();
            response.addHeader("Set-Cookie", jwtCookie.toString());

            return ResponseEntity.noContent().build();

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
