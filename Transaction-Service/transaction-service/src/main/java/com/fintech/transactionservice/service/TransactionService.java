package com.fintech.transactionservice.service;

import com.fintech.transactionservice.dto.TransferRequest;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.exception.DailyLimitExceededException;
import com.fintech.transactionservice.exception.ResourceNotFoundException;
import com.fintech.transactionservice.exception.ServiceUnavailableException;
import com.fintech.transactionservice.repository.TransactionRepository;
import com.fintech.transactionservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * TransactionService — Core of the Transaction-Service.
 *
 * Responsibilities:
 * 1. Validate transfer requests (amount > 0, sender ≠ receiver)
 * 2. Enforce daily transaction limits (USER: ₹50,000 | ADMIN: ₹200,000)
 * 3. Fast-Tracking PENDING persistence.
 * 4. Execute strict atomic manual-rollback transfer.
 * 5. Handle Account-Service failures with robust parsing.
 * 6. High-Fidelity Logging bound to Transaction IDs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    private final JwtService jwtService;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    // ── Daily Limits ────────────────────────────────────────────────────────────
    private static final double USER_DAILY_LIMIT  = 50_000.0;
    private static final double ADMIN_DAILY_LIMIT = 200_000.0;

    // ── Transfer ────────────────────────────────────────────────────────────────

    @Transactional
    public Transaction transfer(TransferRequest request,
                                String initiatedBy,
                                String bearerToken) {

        // ── Step 1: Input Validation ────────────────────────────────────────────
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than 0");
        }

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Sender and receiver accounts cannot be the same");
        }

        // ── Step 1.5: OWNERSHIP VALIDATION (IDOR PROTECTION) ────────────────────
        String senderNameLocal = "Unknown Sender";
        try {
            HttpHeaders checkHeaders = new HttpHeaders();
            checkHeaders.set("Authorization", bearerToken);
            HttpEntity<Void> checkEntity = new HttpEntity<>(checkHeaders);
            
            ResponseEntity<java.util.Map> checkResp = restTemplate.exchange(accountServiceUrl + "/api/accounts/" + request.getFromAccountId(), 
                    HttpMethod.GET, checkEntity, java.util.Map.class);
                    
            if (checkResp.getBody() != null && checkResp.getBody().get("data") != null) {
                java.util.Map<?, ?> accData = (java.util.Map<?, ?>) checkResp.getBody().get("data");
                if (accData.get("name") != null) {
                    senderNameLocal = accData.get("name").toString();
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden ex) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own the source account for this transfer");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Source account not found");
        }

        // ── Step 1.8: TRANSACTION PASSWORD VERIFICATION ─────────────────────────
        try {
            HttpHeaders pwdHeaders = new HttpHeaders();
            pwdHeaders.set("Authorization", bearerToken);
            pwdHeaders.set("Content-Type", "application/json");
            
            java.util.Map<String, String> pwdPayload = new java.util.HashMap<>();
            pwdPayload.put("password", request.getTransactionPassword());
            
            HttpEntity<java.util.Map<String, String>> pwdEntity = new HttpEntity<>(pwdPayload, pwdHeaders);
            
            restTemplate.exchange(accountServiceUrl + "/api/accounts/" + request.getFromAccountId() + "/verify-password",
                    HttpMethod.POST, pwdEntity, Object.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid transaction password");
        }

        // ── Step 2: Daily Limit Check ───────────────────────────────────────────
        String role = extractRoleFromToken(bearerToken);
        double dailyLimit = "ADMIN".equalsIgnoreCase(role) ? ADMIN_DAILY_LIMIT : USER_DAILY_LIMIT;

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Double alreadySpentToday = transactionRepository
                .sumSuccessfulTransfersSince(request.getFromAccountId(), startOfDay,
                        Transaction.TransactionStatus.SUCCESS);
        if (alreadySpentToday == null) alreadySpentToday = 0.0;

        if (alreadySpentToday + request.getAmount() > dailyLimit) {
            log.warn("[DAILY LIMIT] Account {} | spent today={} | requested={} | limit={}",
                    request.getFromAccountId(), alreadySpentToday, request.getAmount(), dailyLimit);
            throw new DailyLimitExceededException(alreadySpentToday, dailyLimit, request.getAmount());
        }

        // ── Step 3: Record PENDING State ─────────────────────────────────────────
        Transaction tx = new Transaction();
        tx.setFromAccountId(request.getFromAccountId());
        tx.setToAccountId(request.getToAccountId());
        tx.setAmount(request.getAmount());
        tx.setType(Transaction.TransactionType.TRANSFER);
        tx.setStatus(Transaction.TransactionStatus.PENDING);
        tx.setInitiatedBy(initiatedBy);
        tx.setTimestamp(LocalDateTime.now());
        tx.setNote(request.getNote());
        tx.setSenderName(senderNameLocal);

        tx = transactionRepository.save(tx);
        Long txId = tx.getId();
        log.info("[TRANSACTION {}] Started by User '{}' | {} -> {} | amount={}", 
                 txId, initiatedBy, request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        // ── Step 4: Execute Inter-Service Calls & Rollback Pipeline ──────────────
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        boolean debitSucceeded = false;

        try {
            // 4a. DEBIT Sender (Withdrawal)
            String withdrawUrl = accountServiceUrl + "/api/accounts/"
                    + request.getFromAccountId() + "/withdraw?amount=" + request.getAmount();
            
            log.info("[TRANSACTION {}] Sending HTTP DEBIT request to Account-Service for Account: {}", txId, request.getFromAccountId());
            ResponseEntity<Map<String, Object>> withdrawResponse = restTemplate.exchange(
                    withdrawUrl, 
                    HttpMethod.PUT, 
                    entity, 
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // Strong Response Verification
            if (!withdrawResponse.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Account-Service returned non-200 code on withdrawal");
            }
            
            debitSucceeded = true;
            log.info("[TRANSACTION {}] HTTP DEBIT Succeeded", txId);

            // 4b. CREDIT Receiver (Deposit)
            String depositUrl = accountServiceUrl + "/api/accounts/"
                    + request.getToAccountId() + "/deposit?amount=" + request.getAmount();
            
            log.info("[TRANSACTION {}] Sending HTTP CREDIT request to Account-Service for Account: {}", txId, request.getToAccountId());
            ResponseEntity<Map<String, Object>> depositResponse = restTemplate.exchange(
                    depositUrl, 
                    HttpMethod.PUT, 
                    entity, 
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // Strong Response Verification
            if (!depositResponse.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Account-Service returned non-200 code on deposit");
            }
            
            String receiverNameLocal = "Unknown Receiver";
            if (depositResponse.getBody() != null && depositResponse.getBody().get("data") != null) {
                Map<?, ?> recvData = (Map<?, ?>) depositResponse.getBody().get("data");
                if (recvData.get("name") != null) {
                    receiverNameLocal = recvData.get("name").toString();
                }
            }
            
            log.info("[TRANSACTION {}] HTTP CREDIT Succeeded to Receiver: {}", txId, receiverNameLocal);

            // ── Step 5: Finalize SUCCESS ─────────────────────────────────────────
            tx.setStatus(Transaction.TransactionStatus.SUCCESS);
            tx.setReceiverName(receiverNameLocal);
            log.info("[TRANSACTION {}] SUCCESSFUL TRANSFER EXECUTION", txId);

        } catch (ResourceAccessException ex) {
            log.error("[TRANSACTION {}] FAILED — Account-Service unreachable: {}", txId, ex.getMessage());
            triggerRollbackIfNecessary(tx, entity, debitSucceeded);
            throw new ServiceUnavailableException("Account-Service", "Service is currently down or timed out. Please try again later.");

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("[TRANSACTION {}] FAILED — Account not found: {}", txId, ex.getMessage());
            triggerRollbackIfNecessary(tx, entity, debitSucceeded);
            throw new ResourceNotFoundException("Account not found — verify fromAccountId and toAccountId");

        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.warn("[TRANSACTION {}] FAILED — HTTP {}: {}", txId, ex.getStatusCode(), ex.getMessage());
            triggerRollbackIfNecessary(tx, entity, debitSucceeded);
            throw new IllegalArgumentException("Account-Service rejected the request: " + ex.getResponseBodyAsString());

        } catch (Exception ex) {
            log.error("[TRANSACTION {}] FAILED — Unexpected error: {}", txId, ex.getMessage());
            triggerRollbackIfNecessary(tx, entity, debitSucceeded);
            throw new RuntimeException("Transaction failed: " + ex.getMessage());
        }

        return transactionRepository.save(tx);
    }

    // ── Rollback Strategy ────────────────────────────────────────────────────────

    private void triggerRollbackIfNecessary(Transaction tx, HttpEntity<Void> entity, boolean debitSucceeded) {
        if (debitSucceeded) {
            log.warn("[TRANSACTION {}] ROLLBACK INITIATED: Attempting to refund Account {}", tx.getId(), tx.getFromAccountId());
            try {
                String refundUrl = accountServiceUrl + "/api/accounts/"
                        + tx.getFromAccountId() + "/deposit?amount=" + tx.getAmount();
                
                restTemplate.exchange(refundUrl, HttpMethod.PUT, entity, Object.class);
                log.info("[TRANSACTION {}] ROLLBACK SUCCESS: Refunded Account {}", tx.getId(), tx.getFromAccountId());
                tx.setNote("FAILED BUT REFUNDED. Original error occurred.");
            } catch (Exception refundEx) {
                log.error("[TRANSACTION {}] CRITICAL ROLLBACK FAILURE for Account {}: {}", tx.getId(), tx.getFromAccountId(), refundEx.getMessage());
                tx.setNote("CRITICAL: FAILED AND REFUND FAILED. Manual intervention required. " + refundEx.getMessage());
            }
        } else {
            log.info("[TRANSACTION {}] No rollback required (Debit never succeeded)", tx.getId());
            tx.setNote("FAILED BEFORE DEBIT.");
        }
        
        tx.setStatus(Transaction.TransactionStatus.FAILED);
        transactionRepository.save(tx);
    }

    // ── Query Methods ────────────────────────────────────────────────────────────

    public List<Transaction> getByAccountId(Long accountId, String bearerToken) {
        log.debug("[TRANSACTION] Fetching history for account {}", accountId);
        
        // Ownership Validation: If the Account-Service throws 403, we block.
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", bearerToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(accountServiceUrl + "/api/accounts/" + accountId, 
                    HttpMethod.GET, entity, Object.class);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this account");
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Account not found");
        }
        
        return transactionRepository.findByFromAccountIdOrToAccountId(accountId, accountId);
    }

    public List<Transaction> getByUsername(String username, String bearerToken) {
        log.debug("[TRANSACTION] Fetching history for user '{}'", username);
        
        java.util.List<Long> userAccountIds = new java.util.ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            if (bearerToken != null) {
                headers.set("Authorization", bearerToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                    accountServiceUrl + "/api/accounts/user/" + username,
                    HttpMethod.GET, entity, java.util.Map.class);
                    
            if (response.getBody() != null && response.getBody().get("data") != null) {
                Object dataObj = response.getBody().get("data");
                if (dataObj instanceof java.util.List) {
                    java.util.List<?> accountsList = (java.util.List<?>) dataObj;
                    for (Object acc : accountsList) {
                        if (acc instanceof java.util.Map) {
                            Object idRaw = ((java.util.Map<?, ?>) acc).get("id");
                            if (idRaw instanceof Number) {
                                userAccountIds.add(((Number) idRaw).longValue());
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[TRANSACTION] Could not retrieve accounts for user '{}': {}", username, ex.getMessage());
        }

        if (userAccountIds.isEmpty()) {
            return transactionRepository.findByInitiatedByOrderByTimestampDesc(username);
        }
        
        return transactionRepository.findByInitiatedByOrAccountIds(username, userAccountIds);
    }

    public List<Transaction> getAll() {
        log.info("[TRANSACTION] Admin: fetching all transactions");
        return transactionRepository.findAllByOrderByTimestampDesc();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private String extractRoleFromToken(String bearerToken) {
        try {
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return jwtService.extractRole(bearerToken.substring(7));
            }
        } catch (Exception ex) {
            log.warn("[JWT PARSER] Could not extract role from token, defaulting to USER");
        }
        return "USER";
    }
}
