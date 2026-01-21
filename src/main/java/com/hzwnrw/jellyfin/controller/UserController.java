package com.hzwnrw.jellyfin.controller;

import com.hzwnrw.jellyfin.model.UserExpiration;
import com.hzwnrw.jellyfin.repository.ExpirationRepository;
import com.hzwnrw.jellyfin.service.JellyfinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final JellyfinService jellyfinService;
    private final ExpirationRepository repository;

    @GetMapping("/")
    public String index(Model model) {
        var jellyfinUsers = jellyfinService.getAllUsers();

        var tracked = repository.findAll().stream()
                .collect(Collectors.toMap(UserExpiration::getJellyfinUserId, u -> u, (a, b) -> a));

        model.addAttribute("users", jellyfinUsers);
        model.addAttribute("tracked", tracked);
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // This matches src/main/resources/templates/login.html
    }

    @PostMapping("/set-expiry")
    public String setExpiry(@RequestParam String userId, @RequestParam String username, @RequestParam String date) {
        UserExpiration exp = new UserExpiration();
        exp.setJellyfinUserId(userId);
        exp.setUsername(username);
        exp.setExpiryDate(LocalDateTime.parse(date));
        exp.setProcessed(false);
        repository.save(exp);
        return "redirect:/";
    }

    @PostMapping("/toggle")
    public String toggle(@RequestParam String userId, @RequestParam boolean disable) {
        jellyfinService.updateDisableStatus(userId, disable);
        
        // If enabling the account, clear the expiry date from database
        if (!disable) {
            repository.deleteById(userId);
        }
        
        return "redirect:/";
    }
}