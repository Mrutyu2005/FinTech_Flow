package com.fintech.accountservice.controller;

import com.fintech.accountservice.dto.ApiResponse;
import com.fintech.accountservice.entity.Account;
import com.fintech.accountservice.entity.User;
import com.fintech.accountservice.repository.AccountRepository;
import com.fintech.accountservice.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoints.
 * All routes require ADMIN role — enforced both at SecurityConfig level
 * and via @PreAuthorize for defence-in-depth.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin-only APIs — requires ADMIN role")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "List all users",
            description = "Returns a list of all registered users. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        log.info("[ADMIN] Fetched {} users", users.size());
        return ResponseEntity.ok(ApiResponse.success(
                "Fetched " + users.size() + " users", users));
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "List all accounts",
            description = "Returns all bank accounts in the system. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Accounts retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<ApiResponse<List<Account>>> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        log.info("[ADMIN] Fetched {} accounts", accounts.size());
        return ResponseEntity.ok(ApiResponse.success(
                "Fetched " + accounts.size() + " accounts", accounts));
    }

    @PutMapping("/users/{username}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Change user role",
            description = "Promote or demote a user's role (USER/ADMIN). Requires ADMIN role."
    )
    public ResponseEntity<ApiResponse<String>> changeRole(
            @PathVariable String username,
            @RequestParam String role) {

        if (!role.equalsIgnoreCase("USER") && !role.equalsIgnoreCase("ADMIN")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Role must be USER or ADMIN"));
        }

        com.fintech.accountservice.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.fintech.accountservice.exception.ResourceNotFoundException(
                        "User not found: " + username));

        user.setRole(role.toUpperCase());
        userRepository.save(user);

        log.info("[ADMIN] Role of user '{}' changed to '{}'", username, role.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(
                "User '" + username + "' role updated to " + role.toUpperCase(), null));
    }
}
