package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.ChargeRequest;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.service.LoanAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Loan Accounts (Credit Cards)
 * API compatible with Fineract format
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class LoanAccountController {

    private final LoanAccountService loanAccountService;

    /**
     * Get loan account details
     * GET /loans/{loanId}
     * Fineract compatible
     */
    @GetMapping("/loans/{loanId}")
    public ResponseEntity<Map<String, Object>> getAccount(@PathVariable String loanId) {
        log.info("GET /loans/{}", loanId);
        
        try {
            LoanAccount account = loanAccountService.getAccount(loanId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", account.getId());
            response.put("accountNo", account.getAccountNumber());
            response.put("accountNumber", account.getAccountNumber());
            response.put("principal", account.getPrincipal());
            response.put("principalOutstanding", account.getPrincipalOutstanding());
            response.put("currency", Map.of("code", account.getCurrency()));
            response.put("status", Map.of("value", account.getStatus()));
            response.put("clientName", account.getClientName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting loan: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add charge to loan (credit card transaction)
     * POST /loans/{loanId}/charges
     * Fineract compatible
     */
    @PostMapping("/loans/{loanId}/charges")
    public ResponseEntity<Map<String, Object>> addCharge(
            @PathVariable String loanId,
            @RequestBody ChargeRequest request) {
        
        log.info("POST /loans/{}/charges - Amount: {}", loanId, request.getAmount());
        
        try {
            LoanAccount account = loanAccountService.addCharge(loanId, request.getAmount());
            
            // Fineract-style response
            Map<String, Object> response = new HashMap<>();
            response.put("resourceId", account.getId());
            response.put("loanId", account.getId());
            response.put("changes", Map.of(
                "principalOutstanding", account.getPrincipalOutstanding()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Add charge failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Make payment to loan (Simplified endpoint)
     * POST /loans/{loanId}/payments
     */
    @PostMapping("/loans/{loanId}/payments")
    public ResponseEntity<Map<String, Object>> makePayment(
            @PathVariable String loanId,
            @RequestBody Map<String, Object> request) {
        
        log.info("POST /loans/{}/payments", loanId);
        
        try {
            Double amount = ((Number) request.get("amount")).doubleValue();
            LoanAccount account = loanAccountService.makePayment(loanId, amount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("resourceId", account.getId());
            response.put("changes", Map.of(
                "principalOutstanding", account.getPrincipalOutstanding()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Payment failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Make payment to loan
     * POST /loans/{loanId}/transactions?command=repayment
     */
    @PostMapping("/loans/{loanId}/transactions")
    public ResponseEntity<Map<String, Object>> transaction(
            @PathVariable String loanId,
            @RequestParam String command,
            @RequestBody Map<String, Object> request) {
        
        log.info("POST /loans/{}/transactions?command={}", loanId, command);
        
        try {
            if (!"repayment".equalsIgnoreCase(command)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid command: " + command));
            }
            
            Double amount = ((Number) request.get("transactionAmount")).doubleValue();
            LoanAccount account = loanAccountService.makePayment(loanId, amount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("resourceId", account.getId());
            response.put("changes", Map.of(
                "principalOutstanding", account.getPrincipalOutstanding()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Payment failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create loan account (Admin endpoint)
     * POST /loans
     * Request body: { "accountNumber": "...", "principal": 10000.0, "clientId": "CLI_001" }
     */
    @PostMapping("/loans")
    public ResponseEntity<Map<String, Object>> createLoan(@RequestBody Map<String, Object> request) {
        log.info("POST /loans - Create loan account");
        
        try {
            String accountNumber = (String) request.get("accountNumber");
            Double principal = request.get("principal") != null 
                ? ((Number) request.get("principal")).doubleValue() 
                : 10000.0;
            String clientId = (String) request.get("clientId");  // Changed from clientName
            
            LoanAccount account = loanAccountService.createAccount(accountNumber, principal, clientId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("resourceId", account.getId());
            response.put("accountNumber", account.getAccountNumber());
            response.put("principal", account.getPrincipal());
            response.put("clientId", clientId);
            response.put("clientName", account.getClientName());  // For backward compatibility
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating loan: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
