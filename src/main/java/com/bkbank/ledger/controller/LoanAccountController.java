package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.ChargeRequest;
import com.bkbank.ledger.dto.response.CreditCardMonthlyStatementResponse;
import com.bkbank.ledger.dto.response.CreditCardStatementSummaryResponse;
import com.bkbank.ledger.dto.response.LoanStatementResponse;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.service.CreditCardStatementService;
import com.bkbank.ledger.service.LoanAccountService;
import com.bkbank.ledger.service.StatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
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
    private final LoanAccountRepository loanAccountRepository;
    private final StatementService statementService;
    private final CreditCardStatementService creditCardStatementService;

    /**
     * List all loan accounts
     * GET /loans
     */
    @GetMapping("/loans")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<Page<Map<String, Object>>> listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LoanAccount> accounts = loanAccountRepository.findAll(pageable);
        Page<Map<String, Object>> result = accounts.map(account -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", account.getAccountNumber());
            m.put("accountNo", account.getAccountNumber());
            m.put("principal", account.getPrincipal());
            m.put("principalOutstanding", account.getPrincipalOutstanding());
            m.put("currency", Map.of("code", account.getCurrency()));
            m.put("status", Map.of("value", account.getStatus().name()));
            m.put("clientName", account.getClientName());
            return m;
        });
        return ResponseEntity.ok(result);
    }


    /**
     * Get loan account details
     * GET /loans/{loanId}
     * Fineract compatible
     */
    @GetMapping("/loans/{loanId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
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
            response.put("status", Map.of("value", account.getStatus().name()));
            response.put("clientName", account.getClientName());
            response.put("billingDayOfMonth", account.getBillingDayOfMonth());
            response.put("paymentDueDays", account.getPaymentDueDays());
            response.put("minimumPaymentRate", account.getMinimumPaymentRate());
            response.put("minimumPaymentFloor", account.getMinimumPaymentFloor());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting loan: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate loan statement by date range
     * GET /loans/{loanId}/statement?fromDate=2026-03-01&toDate=2026-03-31
     */
    @GetMapping("/loans/{loanId}/statement")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> getStatement(
            @PathVariable String loanId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) {
        try {
            LoanStatementResponse statement = statementService.getLoanStatement(loanId, fromDate, toDate);
            return ResponseEntity.ok(statement);
        } catch (Exception e) {
            log.error("Get statement failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate monthly credit-card style statement
     * POST /loans/{loanId}/monthly-statements/generate?billingDate=2026-03-25
     */
    @PostMapping("/loans/{loanId}/monthly-statements/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> generateMonthlyStatement(
            @PathVariable String loanId,
            @RequestParam LocalDate billingDate
    ) {
        try {
            CreditCardMonthlyStatementResponse statement =
                    creditCardStatementService.generateMonthlyStatement(loanId, billingDate);
            return ResponseEntity.ok(statement);
        } catch (Exception e) {
            log.error("Generate monthly statement failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List persisted monthly statement snapshots for a loan account
     * GET /loans/{loanId}/monthly-statements
     */
    @GetMapping("/loans/{loanId}/monthly-statements")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> getMonthlyStatementHistory(@PathVariable String loanId) {
        try {
            List<CreditCardStatementSummaryResponse> statements =
                    creditCardStatementService.getStatementHistory(loanId);
            return ResponseEntity.ok(statements);
        } catch (Exception e) {
            log.error("Get monthly statement history failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get persisted monthly statement detail by billing date
     * GET /loans/{loanId}/monthly-statements/{billingDate}
     */
    @GetMapping("/loans/{loanId}/monthly-statements/{billingDate}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> getMonthlyStatementDetail(
            @PathVariable String loanId,
            @PathVariable LocalDate billingDate
    ) {
        try {
            CreditCardMonthlyStatementResponse statement =
                    creditCardStatementService.getStatementDetail(loanId, billingDate);
            return ResponseEntity.ok(statement);
        } catch (Exception e) {
            log.error("Get monthly statement detail failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add charge to loan (credit card transaction)
     * POST /loans/{loanId}/charges
     * Fineract compatible
     */
    @PostMapping("/loans/{loanId}/charges")
    @PreAuthorize("hasRole('SYSTEM') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> addCharge(
            @PathVariable String loanId,
            @RequestBody ChargeRequest request) {
        
        log.info("POST /loans/{}/charges - Amount: {}", loanId, request.getAmount());
        
        try {
            String merchantId = request.getMerchantId();
            String merchantName = request.getMerchantName();
            String cardNetwork = request.getCardNetwork();
            String location = request.getLocation();
            Double latitude = request.getLatitude();
            Double longitude = request.getLongitude();
            LoanAccount account = loanAccountService.addCharge(
                    loanId,
                    request.getAmount(),
                    merchantId,
                    merchantName,
                    cardNetwork,
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
     * Manage account status (activate, lock, unlock, close)
     * POST /loans/{loanId}?command=<action>
     */
    @PostMapping("/loans/{loanId}")
    public ResponseEntity<Map<String, Object>> manageStatus(
            @PathVariable String loanId,
            @RequestParam String command,
            @RequestBody(required = false) Map<String, Object> request) {
        
        log.info("POST /loans/{}?command={}", loanId, command);
        
        try {
            LoanAccount account;
            
            switch (command.toLowerCase()) {
                case "activate":
                    account = loanAccountService.activateAccount(loanId);
                    break;
                    
                case "lock":
                    String reason = request != null ? (String) request.get("reason") : "No reason provided";
                    account = loanAccountService.lockAccount(loanId, reason);
                    break;
                    
                case "unlock":
                    account = loanAccountService.unlockAccount(loanId);
                    break;
                    
                case "close":
                    account = loanAccountService.closeAccount(loanId);
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
     * Create loan account (Admin endpoint)
     * POST /loans
     * Request body: { "accountNumber": "...", "principal": 10000.0, "clientId": "CLI_001" }
     */
    @PostMapping("/loans")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
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
