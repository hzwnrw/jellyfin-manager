package com.hzwnrw.jellyfin.security;

import com.hzwnrw.jellyfin.filter.JwtAuthenticationFilter;
import com.hzwnrw.jellyfin.service.AppUserDetailsService;
import com.hzwnrw.jellyfin.service.TokenBlacklistService;
import com.hzwnrw.jellyfin.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import jakarta.servlet.http.Cookie;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final AppUserDetailsService appUserDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    // 1. Password Encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Explicitly define the Manager to prevent the StackOverflow loop
    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(appUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    // 3. Custom logout handler for JWT token blacklisting
    @Bean
    public LogoutHandler jwtLogoutHandler() {
        return (request, response, authentication) -> {
            String token = null;

            // Try to get token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            // Fallback: Try to get token from cookie
            if (token == null) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("jwt_token".equals(cookie.getName())) {
                            token = cookie.getValue();
                            break;
                        }
                    }
                }
            }

            // Blacklist the token
            if (token != null && !token.isEmpty()) {
                try {
                    tokenBlacklistService.blacklistToken(token, 86400L);
                    System.out.println("[LOGOUT HANDLER] Token blacklisted successfully");
                } catch (Exception e) {
                    System.out.println("[LOGOUT HANDLER] Failed to blacklist token: " + e.getMessage());
                }
            } else {
                System.out.println("[LOGOUT HANDLER] No token found to blacklist");
            }
        };
    }

    // 3. The Filter Chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disabled for local development/APIs
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/api/auth/**", "/logout", "/css/**", "/js/**", "/static/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(jwtLogoutHandler())
                        .logoutSuccessUrl("/login")
                        .deleteCookies("jwt_token")
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .headers(headers -> headers.cacheControl(cache -> cache.disable()))
                // This is your custom filter that looks for the JWT in the header
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtils, tokenBlacklistService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}