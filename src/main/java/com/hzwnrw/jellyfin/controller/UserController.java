package com.hzwnrw.jellyfin.controller;

import com.hzwnrw.jellyfin.model.UserExpiration;
import com.hzwnrw.jellyfin.repository.ExpirationRepository;
import com.hzwnrw.jellyfin.service.JellyfinService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final JellyfinService jellyfinService;
    private final ExpirationRepository repository;
    
    @Value("${app.timezone:Asia/Kuala_Lumpur}")
    private String defaultTimezone;

    @GetMapping("/")
    public String index(Model model, HttpServletResponse response,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        log.info("Loading index page with user data - page: {}, size: {}, sortBy: {}, direction: {}", page, size, sortBy, direction);
        
        // Create Pageable with sorting
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        var jellyfinUsersPage = jellyfinService.getAllUsers(pageable);
        log.debug("Retrieved {} jellyfin users from page {} of {}", jellyfinUsersPage.getContent().size(), page, jellyfinUsersPage.getTotalPages());

        var tracked = repository.findAll().stream()
                .collect(Collectors.toMap(UserExpiration::getJellyfinUserId, u -> u, (a, b) -> a));
        log.debug("Loaded {} tracked expirations", tracked.size());

        // Format expiry dates for display
        var trackedFormatted = new java.util.HashMap<String, Map<String, String>>();
        for (var entry : tracked.entrySet()) {
            var expiry = entry.getValue();
            var formatted = new java.util.HashMap<String, String>();
            formatted.put("username", expiry.getUsername());
            formatted.put("expiryDateFormatted", com.hzwnrw.jellyfin.utils.TimezoneUtils.formatForDisplay(
                expiry.getExpiryDate(), defaultTimezone));
            trackedFormatted.put(entry.getKey(), formatted);
        }

        // Format current date in the configured timezone
        String currentDateFormatted = ZonedDateTime.now(ZoneId.of(defaultTimezone))
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));

        // Prevent caching
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        model.addAttribute("users", jellyfinUsersPage.getContent());
        model.addAttribute("tracked", trackedFormatted);
        model.addAttribute("timezone", defaultTimezone);
        model.addAttribute("currentDate", currentDateFormatted);
        
        // Add pagination attributes
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", jellyfinUsersPage.getTotalPages());
        model.addAttribute("totalElements", jellyfinUsersPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction.toString());
        model.addAttribute("hasNext", jellyfinUsersPage.hasNext());
        model.addAttribute("hasPrevious", jellyfinUsersPage.hasPrevious());
        
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        log.info("Serving login page");
        return "login"; // This matches src/main/resources/templates/login.html
    }

    @PostMapping("/set-expiry")
    public String setExpiry(@RequestParam String userId, @RequestParam String username, @RequestParam String date) {
        log.info("Setting expiry for user: {} (ID: {}) to {} (timezone: {})", username, userId, date, defaultTimezone);
        
        UserExpiration exp = new UserExpiration();
        exp.setJellyfinUserId(userId);
        exp.setUsername(username);
        
        // Parse the datetime string from frontend (in user's local timezone)
        LocalDateTime localDateTime = LocalDateTime.parse(date);
        // Convert to ZonedDateTime in the configured timezone
        ZonedDateTime userZoneDateTime = localDateTime.atZone(ZoneId.of(defaultTimezone));
        // Convert to UTC for storage
        ZonedDateTime utcDateTime = userZoneDateTime.withZoneSameInstant(ZoneId.of("UTC"));
        
        exp.setExpiryDate(utcDateTime);
        exp.setProcessed(false);
        repository.save(exp);
        
        log.info("Expiry set successfully for user: {} | User timezone ({}, {}): {} | Stored in UTC: {}", 
            username, defaultTimezone, userZoneDateTime.getZone(), userZoneDateTime, utcDateTime);
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

    @PostMapping("/sync")
    public String syncUsers() {
        log.info("Manual sync triggered");
        jellyfinService.syncAndGetAllUsers();
        log.info("Manual sync completed");
        return "redirect:/";
    }
}
