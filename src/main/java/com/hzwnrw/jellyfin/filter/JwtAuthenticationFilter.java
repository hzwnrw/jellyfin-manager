package com.hzwnrw.jellyfin.filter;

import com.hzwnrw.jellyfin.utils.JwtUtils;
import com.hzwnrw.jellyfin.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtils = jwtUtils;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            log.debug("Token found in Authorization header");
        } else {
            // Check for JWT in cookie
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        log.debug("Token found in jwt_token cookie");
                        break;
                    }
                }
            }
        }

        if (token != null && jwtUtils.validateToken(token)) {
            log.debug("Token is valid, checking blacklist status");
            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                log.warn("Attempt to use blacklisted token: {}", token.substring(0, Math.min(20, token.length())) + "...");
                SecurityContextHolder.clearContext();
            } else {
                log.debug("Token is not blacklisted, setting authentication");
                Authentication auth = jwtUtils.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } else {
            if (token == null) {
                log.debug("No token found in request");
            } else {
                log.debug("Token validation failed");
            }
        }

        filterChain.doFilter(request, response);
    }
}