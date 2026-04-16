package com.fintech.accountservice.controller;

import com.fintech.accountservice.dto.ApiResponse;
import com.fintech.accountservice.dto.AccountRequest;
import com.fintech.accountservice.entity.Account;
import com.fintech.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management", description = "APIs for managing bank accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(
            summary = "Create a new bank account",
            description = "Creates a new account for the authenticated user. " +
                    "Account types: SAVINGS, CURRENT, FIXED."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — JWT required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Account>> create(
            @Valid @RequestBody AccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.createAccount(
                userDetails.getUsername(),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", account));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get my account",
            description = "Returns the account belonging to the natively logged-in user JWT."
    )
    public ResponseEntity<ApiResponse<Account>> getMyAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<Account> accounts = accountService.getByUsername(userDetails.getUsername());
        if (accounts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No account found for user"));
        }
        return ResponseEntity.ok(ApiResponse.success("Account retrieved", accounts.get(0)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get account by ID",
            description = "Returns account details for the given account ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<Account>> getById(
            @Parameter(description = "Account ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String role = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "USER";
        
        return ResponseEntity.ok(ApiResponse.success("Account retrieved", 
                accountService.getById(id, userDetails.getUsername(), role)));
    }

    @PostMapping("/{id}/verify-password")
    @Operation(
            summary = "Verify account transaction password",
            description = "Validates the raw transaction password against the encoded DB mapping. Prevents IDOR."
    )
    public ResponseEntity<ApiResponse<Boolean>> verifyTransactionPassword(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
            
        String role = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "USER";
                
        boolean isValid = accountService.verifyTransactionPassword(id, payload.get("password"), userDetails.getUsername(), role);
        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success("Password verified", true));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid transaction password"));
        }
    }

    @GetMapping("/user/{username}")
    @Operation(
            summary = "Get all accounts for a user",
            description = "Returns all accounts owned by the given username."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Accounts retrieved")
    })
    public ResponseEntity<ApiResponse<List<Account>>> getByUsername(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
            
        String role = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "USER";
                
        if (!username.equals(userDetails.getUsername()) && !"ADMIN".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot fetch accounts for other users");
        }
            
        List<Account> accounts = accountService.getByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + accounts.size() + " account(s) for user: " + username, accounts));
    }

    @PutMapping("/{id}/deposit")
    @Operation(
            summary = "Deposit funds",
            description = "Adds funds to the specified account. Amount must be greater than 0."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deposit successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid amount"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<Account>> deposit(
            @PathVariable Long id,
            @Parameter(description = "Amount to deposit (must be > 0)", required = true)
            @RequestParam @Positive(message = "Deposit amount must be positive") Double amount) {
        Account updated = accountService.deposit(id, amount);
        return ResponseEntity.ok(ApiResponse.success(
                "Deposit of " + amount + " successful. New balance: " + updated.getBalance(), updated));
    }

    @PutMapping("/{id}/withdraw")
    @Operation(
            summary = "Withdraw funds",
            description = "Deducts funds from the specified account. Fails if balance is insufficient."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Withdrawal successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid amount"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Insufficient balance")
    })
    public ResponseEntity<ApiResponse<Account>> withdraw(
            @PathVariable Long id,
            @Parameter(description = "Amount to withdraw (must be > 0)", required = true)
            @RequestParam @Positive(message = "Withdrawal amount must be positive") Double amount,
            @AuthenticationPrincipal UserDetails userDetails) {
            
        String role = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "USER";
                
        Account updated = accountService.withdraw(id, amount, userDetails.getUsername(), role);
        return ResponseEntity.ok(ApiResponse.success(
                "Withdrawal of " + amount + " successful. New balance: " + updated.getBalance(), updated));
    }
}
