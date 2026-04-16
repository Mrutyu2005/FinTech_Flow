package com.fintech.transactionservice.controller;

import com.fintech.transactionservice.dto.ApiResponse;
import com.fintech.transactionservice.dto.TransferRequest;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "APIs for fund transfers and transaction history")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(
            summary = "Transfer funds between accounts",
            description = "Transfers money from one account to another. " +
                    "Rules:\n" +
                    "- Amount must be > 0\n" +
                    "- Sender ≠ Receiver\n" +
                    "- Sufficient balance required\n" +
                    "- Daily limit: ₹50,000 (USER), ₹200,000 (ADMIN)\n" +
                    "Both accounts must exist in Account-Service."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Transfer successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request (self-transfer, amount <= 0)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Insufficient balance or daily limit exceeded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Account-Service unavailable")
    })
    public ResponseEntity<ApiResponse<Transaction>> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        String bearerToken = httpRequest.getHeader("Authorization");
        String initiatedBy = userDetails.getUsername();

        log.info("[TRANSFER] Initiated by '{}' | {} -> {} | amount={}",
                initiatedBy, request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        Transaction tx = transactionService.transfer(request, initiatedBy, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer completed with status: " + tx.getStatus(), tx));
    }

    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "Get transaction history for an account",
            description = "Returns all transactions (sent and received) for the specified account ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "History retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<Transaction>>> getByAccountId(
            @Parameter(description = "Account ID to fetch transactions for", required = true)
            @PathVariable Long accountId,
            HttpServletRequest httpRequest) {
            
        String bearerToken = httpRequest.getHeader("Authorization");
        List<Transaction> txList = transactionService.getByAccountId(accountId, bearerToken);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + txList.size() + " transaction(s) for account " + accountId, txList));
    }

    @GetMapping("/user/{username}")
    @Operation(
            summary = "Get transactions by username",
            description = "Returns all transactions initiated by the specified user, ordered by date (newest first)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<Transaction>>> getByUsername(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
            
        String role = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "USER";
                
        if (!username.equals(userDetails.getUsername()) && !"ADMIN".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: cannot view transactions of other users");
        }
            
        String bearerToken = httpRequest.getHeader("Authorization");
        List<Transaction> txList = transactionService.getByUsername(username, bearerToken);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + txList.size() + " transaction(s) for user: " + username, txList));
    }

    @GetMapping("/my")
    @Operation(
            summary = "Get my transactions",
            description = "Returns all transactions corresponding to the JWT identity."
    )
    public ResponseEntity<ApiResponse<List<Transaction>>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        
        String bearerToken = httpRequest.getHeader("Authorization");
        List<Transaction> txList = transactionService.getByUsername(userDetails.getUsername(), bearerToken);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + txList.size() + " personal transaction(s)", txList));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all transactions (ADMIN only)",
            description = "Returns all transactions in the system, ordered by timestamp descending. Requires ADMIN role."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All transactions retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<ApiResponse<List<Transaction>>> getAll() {
        List<Transaction> txList = transactionService.getAll();
        return ResponseEntity.ok(ApiResponse.success(
                "Fetched " + txList.size() + " total transaction(s)", txList));
    }
}
