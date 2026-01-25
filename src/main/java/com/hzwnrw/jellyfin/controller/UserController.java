package com.hzwnrw.jellyfin.controller;

import com.hzwnrw.jellyfin.model.UserExpiration;
import com.hzwnrw.jellyfin.repository.ExpirationRepository;
import com.hzwnrw.jellyfin.service.JellyfinService;
import com.hzwnrw.jellyfin.service.TokenBlacklistService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final JellyfinService jellyfinService;
    private final ExpirationRepository repository;
    private final TokenBlacklistService tokenBlacklistService;

    @GetMapping("/")
    public String index(Model model, HttpServletResponse response) {
        log.info("Loading index page with user data");
        var jellyfinUsers = jellyfinService.getAllUsers();
        log.debug("Retrieved {} jellyfin users", jellyfinUsers.size());

        var tracked = repository.findAll().stream()
                .collect(Collectors.toMap(UserExpiration::getJellyfinUserId, u -> u, (a, b) -> a));
        log.debug("Loaded {} tracked expirations", tracked.size());

        // Prevent caching
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        model.addAttribute("users", jellyfinUsers);
        model.addAttribute("tracked", tracked);
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        log.info("Serving login page");
        return "login"; // This matches src/main/resources/templates/login.html
    }

    @PostMapping("/set-expiry")
    public String setExpiry(@RequestParam String userId, @RequestParam String username, @RequestParam String date) {
        log.info("Setting expiry for user: {} (ID: {}) to {}", username, userId, date);
        UserExpiration exp = new UserExpiration();
        exp.setJellyfinUserId(userId);
        exp.setUsername(username);
        exp.setExpiryDate(LocalDateTime.parse(date));
        exp.setProcessed(false);
        repository.save(exp);
        log.info("Expiry set successfully for user: {}", username);
        return "redirect:/";
    }

    @PostMapping("/toggle")
    public String toggle(@RequestParam String userId, @RequestParam boolean disable) {
        log.info("Toggling user {}: disable={}", userId, disable);
        jellyfinService.updateDisableStatus(userId, disable);

        // If enabling the account, clear the expiry date from database
        if (!disable) {
            repository.deleteById(userId);
            log.info("Cleared expiry for enabled user: {}", userId);
        }

        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        log.warn("===== LOGOUT ENDPOINT CALLED =====");

        String token = null;
        boolean blacklistSuccess = false;

        // Get the JWT token from cookie (primary source)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            log.debug("Number of cookies: {}", cookies.length);
            for (Cookie cookie : cookies) {
                log.debug("Cookie: {} = {}", cookie.getName(), cookie.getValue() != null ? cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "..." : "null");
                if ("jwt_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    log.warn("JWT token extracted from cookie");
                    break;
                }
            }
        } else {
            log.warn("No cookies found in request");
        }

        // Fallback: Check Authorization header
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.warn("JWT token extracted from Authorization header");
            }
        }

        log.warn("Extracted token from request: {}, length: {}", token != null ? token.substring(0, 20) + "..." : "null", token != null ? token.length() : 0);

        // Blacklist the token if present
        if (token != null) {
            try {
                // Blacklist for 1 day (86400 seconds) - match JWT expiration
                tokenBlacklistService.blacklistToken(token, 86400L);
                blacklistSuccess = true;
                log.warn("JWT token blacklisted successfully in Redis");
            } catch (Exception e) {
                log.error("Failed to blacklist JWT token: {}", e.getMessage(), e);
            }
        } else {
            log.error("CRITICAL: Token is null or empty, cannot blacklist!");
        }

        // Clear the security context
        SecurityContextHolder.clearContext();

        // Clear the JWT cookie - must match the exact attributes used when setting
        Cookie clearCookie = new Cookie("jwt_token", "");
        clearCookie.setPath("/");
        clearCookie.setMaxAge(0);
        clearCookie.setHttpOnly(false); // Match the client-side cookie setting
        response.addCookie(clearCookie);
        log.warn("JWT cookie cleared from response");

        log.warn("===== USER LOGGED OUT: token_extracted={}, blacklisted={} =====", token != null ? "yes" : "no", blacklistSuccess ? "yes" : "no");
        return "redirect:/login";
    }
}