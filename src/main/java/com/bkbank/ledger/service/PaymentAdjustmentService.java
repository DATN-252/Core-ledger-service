package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.request.PaymentAdjustmentRequest;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAdjustmentService {

    private final TransactionRepository transactionRepository;
    private final SavingsAccountService savingsAccountService;
    private final LoanAccountService loanAccountService;
    private final SettlementAdjustmentService settlementAdjustmentService;

    @Transactional
    public Transaction applyAdjustment(String adjustmentType, PaymentAdjustmentRequest request) {
        String originalPaymentId = normalize(request.getOriginalPaymentId());
        if (originalPaymentId == null) {
            throw new IllegalArgumentException("originalPaymentId is required");
        }

        Transaction originalTransaction = transactionRepository.findByPaymentId(originalPaymentId)
                .orElseThrow(() -> new RuntimeException("Original transaction not found: " + originalPaymentId));

        validateAdjustmentRequest(adjustmentType, request, originalTransaction);

        Transaction existingByIdempotency = findExistingByIdempotency(
                request.getIdempotencyKey(),
                adjustmentType,
                originalTransaction.getPaymentId()
        );
        if (existingByIdempotency != null) {
            return existingByIdempotency;
        }

        String normalizedAdjustmentType = adjustmentType.toUpperCase();
        Transaction adjustmentTransaction;
        if ("SAVINGS".equalsIgnoreCase(originalTransaction.getAccountType())) {
            adjustmentTransaction = savingsAccountService.applyCardAdjustment(
                    originalTransaction.getAccountNumber(),
                    originalTransaction,
                    normalizedAdjustmentType,
                    request.getReason(),
                    request.getPaymentId(),
                    request.getIdempotencyKey(),
                    firstNonBlank(request.getChannel(), "BACKOFFICE"),
                    request.getExternalReference()
            );
        } else if ("LOAN".equalsIgnoreCase(originalTransaction.getAccountType())) {
            adjustmentTransaction = loanAccountService.applyCardAdjustment(
                    originalTransaction.getAccountNumber(),
                    originalTransaction,
                    normalizedAdjustmentType,
                    request.getReason(),
                    request.getPaymentId(),
                    request.getIdempotencyKey(),
                    firstNonBlank(request.getChannel(), "BACKOFFICE"),
                    request.getExternalReference()
            );
        } else {
            throw new IllegalArgumentException("Unsupported account type for adjustments: " + originalTransaction.getAccountType());
        }

        originalTransaction.setStatus("REVERSAL".equalsIgnoreCase(normalizedAdjustmentType) ? "REVERSED" : "REFUNDED");
        originalTransaction.setResponseMessage(
                request.getReason() != null && !request.getReason().isBlank()
                        ? request.getReason()
                        : normalizedAdjustmentType + " applied"
        );
        transactionRepository.save(originalTransaction);

        settlementAdjustmentService.createPostSettlementAdjustmentIfNeeded(
                originalTransaction,
                adjustmentTransaction,
                normalizedAdjustmentType,
                request.getReason()
        );

        log.info("{} applied successfully for original paymentId={} adjustmentPaymentId={}",
                normalizedAdjustmentType,
                originalPaymentId,
                adjustmentTransaction.getPaymentId());
        return adjustmentTransaction;
    }

    private void validateAdjustmentRequest(String adjustmentType,
                                           PaymentAdjustmentRequest request,
                                           Transaction originalTransaction) {
        if (!"REFUND".equalsIgnoreCase(adjustmentType) && !"REVERSAL".equalsIgnoreCase(adjustmentType)) {
            throw new IllegalArgumentException("Unsupported adjustment type: " + adjustmentType);
        }
        if (!"SUCCESS".equalsIgnoreCase(originalTransaction.getStatus())) {
            throw new IllegalArgumentException("Only successful transactions can be adjusted");
        }
        if (!"WITHDRAWAL".equalsIgnoreCase(originalTransaction.getTransactionType())
                && !"CHARGE".equalsIgnoreCase(originalTransaction.getTransactionType())) {
            throw new IllegalArgumentException("Only card charge/withdrawal transactions can be adjusted");
        }
        if (request.getAmount() != null) {
            double requestedAmount = roundMoney(request.getAmount());
            double originalAmount = roundMoney(safe(originalTransaction.getAmount()));
            if (Double.compare(requestedAmount, originalAmount) != 0) {
                throw new IllegalArgumentException("Partial refund/reversal is not supported yet");
            }
        }
        if (request.getPaymentId() != null && !request.getPaymentId().isBlank()) {
            Transaction existing = transactionRepository.findByPaymentId(request.getPaymentId()).orElse(null);
            if (existing != null && !sameAdjustment(existing, adjustmentType, originalTransaction.getPaymentId())) {
                throw new IllegalArgumentException("paymentId already exists");
            }
        }
    }

    private Transaction findExistingByIdempotency(String idempotencyKey,
                                                  String adjustmentType,
                                                  String originalPaymentId) {
        String normalized = normalize(idempotencyKey);
        if (normalized == null) {
            return null;
        }
        Transaction existing = transactionRepository.findByIdempotencyKey(normalized).orElse(null);
        if (existing == null) {
            return null;
        }
        if (sameAdjustment(existing, adjustmentType, originalPaymentId)) {
            return existing;
        }
        throw new IllegalArgumentException("idempotencyKey already exists for a different transaction");
    }

    private boolean sameAdjustment(Transaction existing, String adjustmentType, String originalPaymentId) {
        return adjustmentType.equalsIgnoreCase(existing.getTransactionType())
                && originalPaymentId.equals(existing.getOriginalTransactionId());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }
}

