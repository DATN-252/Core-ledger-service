package com.bkbank.ledger.controller;

import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.SavingsAccountRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import com.bkbank.ledger.repository.spec.LedgerListSpecifications;
import com.bkbank.ledger.service.TransactionLoggingService;
import com.bkbank.ledger.util.PageableSortUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private static final Map<String, String> TRANSACTION_SORT_MAPPINGS = Map.ofEntries(
            Map.entry("date", "transactionDate"),
            Map.entry("transactiondate", "transactionDate"),
            Map.entry("amount", "amount"),
            Map.entry("type", "transactionType"),
            Map.entry("transactiontype", "transactionType"),
            Map.entry("merchantname", "merchantName"),
            Map.entry("merchantid", "merchantId"),
            Map.entry("branchid", "branchId"),
            Map.entry("branchname", "branchName"),
            Map.entry("status", "status"),
            Map.entry("accountnumber", "accountNumber")
    );

    private final TransactionRepository transactionRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
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
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "type") String transactionType,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String branchId,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageableSortUtils.createPageable(page, size, sortBy, sortDir, "transactionDate", TRANSACTION_SORT_MAPPINGS);
        Page<Transaction> txns = transactionRepository.findAll(
                LedgerListSpecifications.transactionList(q, status, transactionType, accountType, branchId),
                pageable
        );
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
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageableSortUtils.createPageable(page, size, sortBy, sortDir, "transactionDate", TRANSACTION_SORT_MAPPINGS);
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
                        .body(new LinkedHashMap<>(Map.of(
                                "error", "Transaction not found",
                                "id", id
                        ))));
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
                        .body(new LinkedHashMap<>(Map.of(
                                "error", "Transaction not found",
                                "paymentId", paymentId
                        ))));
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
                        .body(new LinkedHashMap<>(Map.of(
                                "error", "Transaction not found",
                                "idempotencyKey", idempotencyKey
                        ))));
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
        String paymentNote = (String) body.getOrDefault("paymentNote", null);
        String currency = (String) body.getOrDefault("currency", resolveAccountCurrency(accountNumber, accountType));

        Transaction tx;
        if ("SAVINGS".equalsIgnoreCase(accountType)) {
            tx = Transaction.createFailedWithdrawal(accountNumber, amount, currency, currentBalance, merchantId, merchantName, location, latitude, longitude, failureReason);
            savingsAccountRepository.findByAccountNumber(accountNumber)
                    .ifPresent(account -> tx.assignBranch(account.getBranchId(), account.getBranchName()));
        } else {
            tx = Transaction.createFailedCharge(accountNumber, amount, currency, currentBalance, merchantId, merchantName, location, latitude, longitude, failureReason);
            loanAccountRepository.findByAccountNumber(accountNumber)
                    .ifPresent(account -> tx.assignBranch(account.getBranchId(), account.getBranchName()));
        }
        tx.setCardNetwork(cardNetwork);
        tx.applyReferenceData(paymentId, idempotencyKey, originalTransactionId, channel, authCode, stan, rrn, externalReference, responseCode, responseMessage);
        if (paymentNote != null && !paymentNote.isBlank()) {
            String baseDescription = tx.getDescription();
            tx.setDescription((baseDescription != null && !baseDescription.isBlank() ? baseDescription + " - " : "") + paymentNote.trim());
        }

        transactionLoggingService.logTransaction(tx);
        log.info("Logged failed transaction for account {} - reason: {}", accountNumber, failureReason);
        return ResponseEntity.ok(Map.of("status", "logged"));
    }

    public record BehavioralFeaturesRequest(
            String accountId,
            Double amount,
            Double latitude,
            Double longitude,
            Long unixTime
    ) {}

    @PostMapping("/system/behavioral-features")
    public ResponseEntity<?> getBehavioralFeatures(
            @RequestHeader("X-System-Api-Key") String apiKey,
            @RequestBody BehavioralFeaturesRequest requestBody
    ) {
        if (!systemApiKey.equals(apiKey)) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        String accountId = requestBody.accountId();
        Double amount = requestBody.amount();
        Double latitude = requestBody.latitude();
        Double longitude = requestBody.longitude();
        Long unixTime = requestBody.unixTime();

        LocalDateTime currentDateTime = LocalDateTime.ofEpochSecond(unixTime, 0, ZoneOffset.UTC);
        LocalDateTime thirtyDaysAgo = currentDateTime.minusDays(30);
        LocalDateTime twentyFourHoursAgo = currentDateTime.minusHours(24);

        // 1. amt_diff_avg_30d
        Double avg30d = transactionRepository.getAverageAmountSince(accountId, thirtyDaysAgo);
        if (avg30d == null) {
            avg30d = 0.0;
        }
        Double amtDiffAvg30d = amount - avg30d;

        // 2. trans_count_24h
        long count24h = transactionRepository.countTransactionsSince(accountId, twentyFourHoursAgo);
        double transCount24h = (double) (count24h + 1);

        // 3. distance_velocity
        double distanceVelocity = 0.0;
        java.util.Optional<Transaction> prevTxOpt = transactionRepository
                .findTopByAccountNumberAndAccountTypeAndTransactionDateBeforeOrderByTransactionDateDesc(
                        accountId,
                        determineAccountType(accountId),
                        currentDateTime
                );

        if (prevTxOpt.isPresent()) {
            Transaction prevTx = prevTxOpt.get();
            if (prevTx.getLatitude() != null && prevTx.getLongitude() != null && latitude != null && longitude != null) {
                double distance = haversine(
                        latitude, longitude,
                        prevTx.getLatitude(), prevTx.getLongitude()
                );
                
                long prevUnix = prevTx.getTransactionDate().toEpochSecond(ZoneOffset.UTC);
                double timeDiffH = (double) (unixTime - prevUnix) / 3600.0;
                
                if (timeDiffH > 0) {
                    distanceVelocity = distance / timeDiffH;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amt_diff_avg_30d", amtDiffAvg30d);
        result.put("trans_count_24h", transCount24h);
        result.put("distance_velocity", distanceVelocity);

        return ResponseEntity.ok(result);
    }

    private String determineAccountType(String accountId) {
        if (accountId.startsWith("SAVINGS") || savingsAccountRepository.existsByAccountNumber(accountId)) {
            return "SAVINGS";
        }
        return "LOAN";
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String resolveAccountCurrency(String accountNumber, String accountType) {
        if ("SAVINGS".equalsIgnoreCase(accountType)) {
            return savingsAccountRepository.findByAccountNumber(accountNumber)
                    .map(account -> account.getCurrency())
                    .orElse("USD");
        }
        return loanAccountRepository.findByAccountNumber(accountNumber)
                .map(account -> account.getCurrency())
                .orElse("USD");
    }
}
