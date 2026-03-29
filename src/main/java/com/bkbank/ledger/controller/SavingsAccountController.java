package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.WithdrawalRequest;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.repository.SavingsAccountRepository;
import com.bkbank.ledger.repository.spec.LedgerListSpecifications;
import com.bkbank.ledger.service.SavingsAccountService;
import com.bkbank.ledger.util.PageableSortUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Savings Accounts (Debit Cards)
 * API compatible with Fineract format
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SavingsAccountController {

    private static final Map<String, String> SAVINGS_SORT_MAPPINGS = Map.ofEntries(
            Map.entry("accountnumber", "accountNumber"),
            Map.entry("clientname", "client.fullName"),
            Map.entry("balance", "balance"),
            Map.entry("currency", "currency"),
            Map.entry("status", "status"),
            Map.entry("branchid", "branch.branchId"),
            Map.entry("branchname", "branch.branchName"),
            Map.entry("createdat", "createdAt"),
            Map.entry("updatedat", "updatedAt")
    );

    private final SavingsAccountService savingsAccountService;
    private final SavingsAccountRepository savingsAccountRepository;

    /**
     * List all savings accounts
     * GET /savingsaccounts
     */
    @GetMapping("/savingsaccounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<Page<Map<String, Object>>> listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageableSortUtils.createPageable(page, size, sortBy, sortDir, "createdAt", SAVINGS_SORT_MAPPINGS);
        Page<SavingsAccount> accounts = savingsAccountRepository.findAll(
                LedgerListSpecifications.savingsList(q, status),
                pageable
        );
        Page<Map<String, Object>> result = accounts.map(account -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", account.getAccountNumber());
            m.put("balance", account.getBalance());
            m.put("currency", account.getCurrency());
            m.put("status", account.getStatus().name());
            m.put("clientName", account.getClientName());
            m.put("branchId", account.getBranchId());
            m.put("branchName", account.getBranchName());
            m.put("createdAt", account.getCreatedAt());
            return m;
        });
        return ResponseEntity.ok(result);
    }

    /**
     * Get savings account details
     * GET /savingsaccounts/{accountId}
     * Fineract compatible
     */
    @GetMapping("/savingsaccounts/{accountId}")
    public ResponseEntity<Map<String, Object>> getAccount(@PathVariable String accountId) {
        log.info("GET /savingsaccounts/{}", accountId);
        
        try {
            SavingsAccount account = savingsAccountService.getAccount(accountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", account.getId());
            response.put("accountNo", account.getAccountNumber());
            response.put("accountNumber", account.getAccountNumber()); // Alternative field
            response.put("accountBalance", account.getBalance());
            response.put("currency", Map.of("code", account.getCurrency()));
            response.put("status", Map.of("value", account.getStatus().name()));
            response.put("clientName", account.getClientName());
            response.put("branchId", account.getBranchId());
            response.put("branchName", account.getBranchName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting account: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Process transaction (withdrawal/deposit)
     * POST /savingsaccounts/{accountId}/transactions?command=withdrawal
     * Fineract compatible
     */
    @PostMapping("/savingsaccounts/{accountId}/transactions")
    public ResponseEntity<Map<String, Object>> transaction(
            @PathVariable String accountId,
            @RequestParam String command,
            @RequestBody WithdrawalRequest request) {
        
        log.info("POST /savingsaccounts/{}/transactions?command={}", accountId, command);
        log.info("Request: {}", request);
        
        try {
            SavingsAccount account;
            
            if ("withdrawal".equalsIgnoreCase(command)) {
                String merchantId = request.getMerchantId();
                String merchantName = request.getMerchantName();
                String location = request.getLocation();
                Double latitude = request.getLatitude();
                Double longitude = request.getLongitude();
                account = savingsAccountService.withdraw(
                        accountId,
                        request.getTransactionAmount(),
                        merchantId,
                        merchantName,
                        location,
                        latitude,
                        longitude,
                        request.getPaymentId(),
                        request.getIdempotencyKey(),
                        request.getOriginalTransactionId(),
                        request.getChannel(),
                        request.getAuthCode(),
                        request.getStan(),
                        request.getRrn(),
                        request.getExternalReference(),
                        request.getResponseCode(),
                        request.getResponseMessage()
                );
            } else if ("deposit".equalsIgnoreCase(command)) {
                account = savingsAccountService.deposit(accountId, request.getTransactionAmount());
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid command: " + command));
            }
            
            // Fineract-style response
            Map<String, Object> response = new HashMap<>();
            response.put("resourceId", account.getId());
            response.put("changes", Map.of(
                "accountBalance", account.getBalance()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Transaction failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manage account status (activate, lock, unlock, close)
     * POST /savingsaccounts/{accountId}?command=<action>
     */
    @PostMapping("/savingsaccounts/{accountId}")
    public ResponseEntity<Map<String, Object>> manageStatus(
            @PathVariable String accountId,
            @RequestParam String command,
            @RequestBody(required = false) Map<String, Object> request) {
        
        log.info("POST /savingsaccounts/{}?command={}", accountId, command);
        
        try {
            SavingsAccount account;
            
            switch (command.toLowerCase()) {
                case "activate":
                    account = savingsAccountService.activateAccount(accountId);
                    break;
                    
                case "lock":
                    String reason = request != null ? (String) request.get("reason") : "No reason provided";
                    account = savingsAccountService.lockAccount(accountId, reason);
                    break;
                    
                case "unlock":
                    account = savingsAccountService.unlockAccount(accountId);
                    break;
                    
                case "close":
                    account = savingsAccountService.closeAccount(accountId);
                    break;
                    
                default:
                    return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid command: " + command + ". Valid commands: activate, lock, unlock, close")
                    );
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("resourceId", account.getId());
            response.put("accountNumber", account.getAccountNumber());
            response.put("status", account.getStatus());
            response.put("message", "Account " + command + "d successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.error("Status management failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create savings account (Admin endpoint)
     * POST /savingsaccounts
     * Request body: { "accountNumber": "...", "balance": 0.0, "clientId": "CLI_001" }
     */
    @PostMapping("/savingsaccounts")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody Map<String, Object> request) {
        log.info("POST /savingsaccounts - Create account");
        
        try {
            String accountNumber = (String) request.get("accountNumber");
            Double balance = request.get("balance") != null 
                ? ((Number) request.get("balance")).doubleValue() 
                : 0.0;
            String clientId = (String) request.get("clientId");  // Changed from clientName
            
            SavingsAccount account = savingsAccountService.createAccount(accountNumber, balance, clientId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("resourceId", account.getId());
            response.put("accountNumber", account.getAccountNumber());
            response.put("clientId", clientId);
            response.put("clientName", account.getClientName());  // For backward compatibility
            response.put("branchId", account.getBranchId());
            response.put("branchName", account.getBranchName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating account: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
