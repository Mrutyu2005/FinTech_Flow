package com.fintech.accountservice.service;

import com.fintech.accountservice.dto.AuthResponse;
import com.fintech.accountservice.dto.LoginRequest;
import com.fintech.accountservice.dto.RegisterRequest;
import com.fintech.accountservice.entity.User;
import com.fintech.accountservice.exception.DuplicateResourceException;
import com.fintech.accountservice.repository.UserRepository;
import com.fintech.accountservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public String register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("[AUTH] Registration failed — username '{}' already exists", request.getUsername());
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER"); // Default role on registration
        userRepository.save(user);

        log.info("[AUTH] User '{}' registered successfully with role=USER", request.getUsername());
        return "User registered successfully";
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Include role in the JWT token for RBAC across services
        String token = jwtService.generateToken(user.getUsername(), user.getRole());

        log.info("[AUTH] User '{}' logged in successfully with role='{}'",
                user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole(), "Login successful");
    }
}
