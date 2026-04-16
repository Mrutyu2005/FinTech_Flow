package com.fintech.transactionservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Transaction-Service Security Config.
 *
 * This service validates JWT tokens issued by Account-Service.
 * It does NOT manage its own users. Role information is read
 * directly from the JWT token claims — no DB lookup required.
 *
 * RBAC:
 *   PUBLIC     → /swagger-ui/**, /v3/api-docs/**
 *   ADMIN only → GET /api/transactions (all transactions view)
 *   USER+      → POST /api/transactions/transfer, GET /api/transactions/user/**, /account/**
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Stub UserDetailsService — the actual user identity and role come
     * from the JWT token. This bean satisfies the Spring Security contract
     * but is never invoked for DB lookup in this service.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("{noop}irrelevant")
                .roles("USER")
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public — Swagger docs
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Admin only — view ALL transactions
                        .requestMatchers("/api/transactions").hasRole("ADMIN")
                        // Any authenticated user — transfers and history
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
