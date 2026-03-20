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
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                transactionRepository.findByAccountNumberOrderByTransactionDateDesc(accountId, pageable)
        );
    }

    /**
     * Get transaction by internal id
     * GET /transactions/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return transactionRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of(
                                "error", "Transaction not found",
                                "id", id
                        )));
    }

    /**
     * Get transaction by paymentId
     * GET /transactions?paymentId=PAY-20260312-000123
     */
    @GetMapping(params = "paymentId")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> getByPaymentId(@RequestParam String paymentId) {
        return transactionRepository.findByPaymentId(paymentId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of(
                                "error", "Transaction not found",
                                "paymentId", paymentId
                        )));
    }

    /**
     * Get transaction by idempotencyKey
     * GET /transactions?idempotencyKey=checkout-order-123
     */
    @GetMapping(params = "idempotencyKey")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<?> getByIdempotencyKey(@RequestParam String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of(
                                "error", "Transaction not found",
                                "idempotencyKey", idempotencyKey
                        )));
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
        String paymentId = (String) body.getOrDefault("paymentId", null);
        String idempotencyKey = (String) body.getOrDefault("idempotencyKey", null);
        String originalTransactionId = (String) body.getOrDefault("originalTransactionId", null);
        String channel = (String) body.getOrDefault("channel", "SYSTEM");
        String authCode = (String) body.getOrDefault("authCode", null);
        String stan = (String) body.getOrDefault("stan", null);
        String rrn = (String) body.getOrDefault("rrn", null);
        String externalReference = (String) body.getOrDefault("externalReference", null);
        String responseCode = (String) body.getOrDefault("responseCode", "96");
        String responseMessage = (String) body.getOrDefault("responseMessage", failureReason);

        Transaction tx;
        if ("SAVINGS".equalsIgnoreCase(accountType)) {
            tx = Transaction.createFailedWithdrawal(accountNumber, amount, currentBalance, merchantId, merchantName, location, latitude, longitude, failureReason);
        } else {
            tx = Transaction.createFailedCharge(accountNumber, amount, currentBalance, merchantId, merchantName, location, latitude, longitude, failureReason);
        }
        tx.setCardNetwork(cardNetwork);
        tx.applyReferenceData(paymentId, idempotencyKey, originalTransactionId, channel, authCode, stan, rrn, externalReference, responseCode, responseMessage);

        transactionLoggingService.logTransaction(tx);
        log.info("Logged failed transaction for account {} - reason: {}", accountNumber, failureReason);
        return ResponseEntity.ok(Map.of("status", "logged"));
    }
}
