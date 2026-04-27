package com.hzwnrw.jellyfin.security;

import com.hzwnrw.jellyfin.filter.JwtAuthenticationFilter;
import com.hzwnrw.jellyfin.service.AppUserDetailsService;
import com.hzwnrw.jellyfin.service.TokenBlacklistService;
import com.hzwnrw.jellyfin.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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

    @Value("${app.security.cookie-secure:false}")
    private boolean secureCookie;

    @Value("${app.security.cookie-same-site:Lax}")
    private String sameSite;

    // 1. Password Encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Explicitly define the Manager to prevent the StackOverflow loop
    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(appUserDetailsService);
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

            if (token != null && !token.isEmpty()) {
                try {
                    tokenBlacklistService.blacklistToken(token, jwtUtils.getRemainingValiditySeconds(token));
                } catch (Exception ignored) {
                }
            }
        };
    }

    // 3. The Filter Chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");

        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/logout",
                                "/api/auth/**",
                                "/favicon.ico",
                                "/app.css",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/static/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(jwtLogoutHandler())
                        .logoutSuccessHandler((request, response, authentication) -> {
                            ResponseCookie clearCookie = ResponseCookie.from("jwt_token", "")
                                    .httpOnly(true)
                                    .secure(secureCookie)
                                    .sameSite(sameSite)
                                    .path("/")
                                    .maxAge(0)
                                    .build();
                            response.addHeader("Set-Cookie", clearCookie.toString());
                            response.sendRedirect("/login");
                        })
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .headers(headers -> headers.cacheControl(cache -> cache.disable()))
                // This is your custom filter that looks for the JWT in the header
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtils, tokenBlacklistService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
