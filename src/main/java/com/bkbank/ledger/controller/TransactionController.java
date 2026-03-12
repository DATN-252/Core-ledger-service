package com.bkbank.ledger.controller;

import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.TransactionRepository;
import com.bkbank.ledger.service.TransactionLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final TransactionLoggingService transactionLoggingService;

    @Value("${system.api-key}")
    private String systemApiKey;

    /**
     * Get all transactions (for UI dashboard)
     * GET /transactions
     * Returns latest 50 transactions ordered by date desc
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<Page<Transaction>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> txns = transactionRepository.findAll(pageable);
        return ResponseEntity.ok(txns);
    }

    /**
     * Get transactions for a specific account
     * GET /transactions?accountId=LOAN_001
     */
    @GetMapping(params = "accountId")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<Page<Transaction>> getByAccount(
            @RequestParam String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                transactionRepository.findByAccountNumberOrderByTransactionDateDesc(accountId, pageable)
        );
    }

    /**
     * Log a failed (declined) transaction — called by CMS service
     * POST /transactions/log-failed
     * Authenticated by X-System-Api-Key header
     */
    @PostMapping("/log-failed")
    public ResponseEntity<?> logFailed(
            @RequestHeader("X-System-Api-Key") String apiKey,
            @RequestBody Map<String, Object> body) {

        if (!systemApiKey.equals(apiKey)) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        String accountNumber = (String) body.get("accountNumber");
        String accountType = (String) body.getOrDefault("accountType", "LOAN");
        Double amount = body.get("amount") instanceof Number ? ((Number) body.get("amount")).doubleValue() : 0.0;
        Double currentBalance = body.get("currentBalance") instanceof Number ? ((Number) body.get("currentBalance")).doubleValue() : 0.0;
        String merchantId = (String) body.getOrDefault("merchantId", "");
        String merchantName = (String) body.getOrDefault("merchantName", "");
        String failureReason = (String) body.getOrDefault("failureReason", "Declined");
        String cardNetwork = (String) body.getOrDefault("cardNetwork", "UNKNOWN");
        String location = (String) body.getOrDefault("location", null);
        Double latitude = body.get("latitude") instanceof Number ? ((Number) body.get("latitude")).doubleValue() : null;
        Double longitude = body.get("longitude") instanceof Number ? ((Number) body.get("longitude")).doubleValue() : null;

        Transaction tx;
        if ("SAVINGS".equalsIgnoreCase(accountType)) {
            tx = Transaction.createFailedWithdrawal(accountNumber, amount, currentBalance, merchantId, merchantName, location, latitude, longitude, failureReason);
        } else {
            tx = Transaction.createFailedCharge(accountNumber, amount, currentBalance, merchantId, merchantName, location, latitude, longitude, failureReason);
        }
        tx.setCardNetwork(cardNetwork);

        transactionLoggingService.logTransaction(tx);
        log.info("Logged failed transaction for account {} - reason: {}", accountNumber, failureReason);
        return ResponseEntity.ok(Map.of("status", "logged"));
    }
}
