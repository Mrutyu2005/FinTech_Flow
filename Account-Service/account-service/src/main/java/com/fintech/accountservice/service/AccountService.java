package com.fintech.accountservice.service;

import com.fintech.accountservice.entity.Account;
import com.fintech.accountservice.exception.InsufficientBalanceException;
import com.fintech.accountservice.exception.ResourceNotFoundException;
import com.fintech.accountservice.repository.AccountRepository;
import com.fintech.accountservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Account createAccount(String ownerUsername, com.fintech.accountservice.dto.AccountRequest request) {
        // Validate that the user exists before creating an account
        userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + ownerUsername));

        Account account = new Account();
        account.setAccountNumber("ACC-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase());
        account.setOwnerUsername(ownerUsername);
        account.setAccountType(request.getAccountType().toUpperCase());
        account.setBalance(request.getInitialBalance());
        
        account.setName(request.getName());
        account.setAddress(request.getAddress());
        account.setMobile(request.getMobile());
        account.setAadhaar(request.getAadhaar());
        account.setEmail(request.getEmail());
        account.setTransactionPassword(passwordEncoder.encode(request.getTransactionPassword()));
        
        Account saved = accountRepository.save(account);

        log.info("[ACCOUNT] Account {} ({}) created for user '{}' with balance={}",
                saved.getAccountNumber(), request.getAccountType(), ownerUsername, request.getInitialBalance());
        return saved;
    }

    public Account getById(Long id, String requestingUsername, String requestingRole) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        
        if (!account.getOwnerUsername().equals(requestingUsername) && !"ADMIN".equalsIgnoreCase(requestingRole)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: you do not own this account");
        }
        return account;
    }

    // Overload for internal trusted calls (like deposit where sender doesn't own receiver)
    public Account getByIdInternal(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    public boolean verifyTransactionPassword(Long id, String rawPassword, String requestingUsername, String requestingRole) {
        Account account = getById(id, requestingUsername, requestingRole);
        return passwordEncoder.matches(rawPassword, account.getTransactionPassword());
    }

    public List<Account> getByUsername(String username) {
        List<Account> accounts = accountRepository.findByOwnerUsername(username);
        log.debug("[ACCOUNT] Fetched {} accounts for user '{}'", accounts.size(), username);
        return accounts;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public Account deposit(Long id, Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0");
        }
        // Deposits can be made to accounts not owned by the initiator (during transfers)
        Account account = getByIdInternal(id);
        double oldBalance = account.getBalance();
        account.setBalance(oldBalance + amount);
        Account saved = accountRepository.save(account);

        log.info("[ACCOUNT] Deposit of {} into account {} | balance: {} -> {}",
                amount, account.getAccountNumber(), oldBalance, saved.getBalance());
        return saved;
    }

    @Transactional
    public Account withdraw(Long id, Double amount, String requestingUsername, String requestingRole) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than 0");
        }
        // Withdrawals MUST be strictly from owned accounts
        Account account = getById(id, requestingUsername, requestingRole);
        if (account.getBalance() < amount) {
            log.warn("[ACCOUNT] Insufficient balance for account {} | available={} requested={}",
                    account.getAccountNumber(), account.getBalance(), amount);
            throw new InsufficientBalanceException(account.getBalance(), amount);
        }
        double oldBalance = account.getBalance();
        account.setBalance(oldBalance - amount);
        Account saved = accountRepository.save(account);

        log.info("[ACCOUNT] Withdrawal of {} from account {} | balance: {} -> {}",
                amount, account.getAccountNumber(), oldBalance, saved.getBalance());
        return saved;
    }
}