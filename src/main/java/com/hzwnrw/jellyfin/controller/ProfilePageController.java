package com.hzwnrw.jellyfin.controller;

import com.hzwnrw.jellyfin.dto.ProfileResponse;
import com.hzwnrw.jellyfin.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProfilePageController {

    private final ProfileService profileService;

    @GetMapping("/profile")
    public String profilePage(Model model) {
        String username = getCurrentUsername();
        log.info("Loading profile page for user: {}", username);
        
        ProfileResponse profile = profileService.getProfile(username);
        model.addAttribute("profile", profile);
        
        return "profile";
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
