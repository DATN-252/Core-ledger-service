package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.response.MerchantSettlementBatchResponse;
import com.bkbank.ledger.dto.response.MerchantSettlementPreviewResponse;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.entity.MerchantSettlementBatch;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.MerchantSettlementBatchRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final MerchantService merchantService;
    private final TransactionRepository transactionRepository;
    private final MerchantSettlementBatchRepository merchantSettlementBatchRepository;
    private final SavingsAccountService savingsAccountService;

    public MerchantSettlementPreviewResponse previewSettlement(String merchantId,
                                                              LocalDate fromDate,
                                                              LocalDate toDate,
                                                              Double feeRate) {
        MerchantSettlementPreviewResponse preview = buildPreview(merchantId, fromDate, toDate, feeRate);
        preview.setSettlementAccountBalance(merchantService.getActiveMerchant(merchantId).getResolvedSettlementAccountBalance());
        return preview;
    }

    @Transactional
    public MerchantSettlementBatchResponse generateSettlementBatch(String merchantId,
                                                                   LocalDate fromDate,
                                                                   LocalDate toDate,
                                                                   Double feeRate,
                                                                   String note) {
        MerchantSettlementPreviewResponse preview = buildPreview(merchantId, fromDate, toDate, feeRate);
        if (preview.getTransactionCount() == null || preview.getTransactionCount() <= 0) {
            throw new IllegalArgumentException("No eligible transactions found for settlement");
        }

        boolean duplicateExists = merchantSettlementBatchRepository.existsByMerchantIdAndFromDateAndToDateAndStatusIn(
                merchantId,
                fromDate,
                toDate,
                List.of(
                        MerchantSettlementBatch.SettlementStatus.PENDING,
                        MerchantSettlementBatch.SettlementStatus.SETTLED
                )
        );
        if (duplicateExists) {
            throw new IllegalArgumentException("Settlement batch already exists for this merchant and date range");
        }

        MerchantSettlementBatch batch = new MerchantSettlementBatch();
        batch.setMerchantId(preview.getMerchantId());
        batch.setMerchantName(preview.getMerchantName());
        batch.setSettlementAccountNumber(preview.getSettlementAccountNumber());
        batch.setSettlementAccountName(preview.getSettlementAccountName());
        batch.setSettlementBankName(preview.getSettlementBankName());
        batch.setCurrency(preview.getCurrency());
        batch.setFromDate(preview.getFromDate());
        batch.setToDate(preview.getToDate());
        batch.setTransactionCount(preview.getTransactionCount());
        batch.setGrossAmount(preview.getGrossAmount());
        batch.setFeeRate(preview.getFeeRate());
        batch.setFeeAmount(preview.getFeeAmount());
        batch.setNetAmount(preview.getNetAmount());
        batch.setStatus(MerchantSettlementBatch.SettlementStatus.PENDING);
        batch.setNote(note);
        batch.setExecutionReference("SET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());

        return toBatchResponse(merchantSettlementBatchRepository.save(batch), preview.getSettlementAccountBalance());
    }

    @Transactional
    public MerchantSettlementBatchResponse executeSettlementBatch(String merchantId,
                                                                  Long batchId,
                                                                  String note) {
        MerchantSettlementBatch batch = merchantSettlementBatchRepository.findByIdAndMerchantId(batchId, merchantId)
                .orElseThrow(() -> new RuntimeException("Settlement batch not found"));

        if (batch.getStatus() != MerchantSettlementBatch.SettlementStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING settlement batches can be executed");
        }

        if (batch.getNetAmount() == null || batch.getNetAmount() <= 0) {
            throw new IllegalArgumentException("Settlement batch has no positive net amount to deposit");
        }

        Merchant merchant = merchantService.getActiveMerchant(merchantId);
        String settlementAccountNumber = merchant.getResolvedSettlementAccountNumber();
        if (settlementAccountNumber == null || settlementAccountNumber.isBlank()) {
            throw new IllegalArgumentException("Merchant settlement account is not configured");
        }

        SavingsAccount settlementAccount = savingsAccountService.depositSettlement(
                settlementAccountNumber,
                batch.getNetAmount(),
                merchant.getMerchantId(),
                merchant.getName(),
                batch.getExecutionReference(),
                note != null && !note.isBlank() ? note : batch.getNote()
        );

        batch.setStatus(MerchantSettlementBatch.SettlementStatus.SETTLED);
        batch.setExecutedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            batch.setNote(note);
        }
        batch = merchantSettlementBatchRepository.save(batch);

        return toBatchResponse(batch, settlementAccount.getBalance());
    }

    public Page<MerchantSettlementBatchResponse> getSettlementBatches(String merchantId, Pageable pageable) {
        merchantService.getActiveMerchant(merchantId);
        return merchantSettlementBatchRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
                .map(batch -> toBatchResponse(batch, resolveCurrentSettlementBalance(merchantId, batch.getSettlementAccountNumber())));
    }

    public MerchantSettlementBatchResponse getSettlementBatch(String merchantId, Long batchId) {
        MerchantSettlementBatch batch = merchantSettlementBatchRepository.findByIdAndMerchantId(batchId, merchantId)
                .orElseThrow(() -> new RuntimeException("Settlement batch not found"));
        return toBatchResponse(batch, resolveCurrentSettlementBalance(merchantId, batch.getSettlementAccountNumber()));
    }

    private MerchantSettlementPreviewResponse buildPreview(String merchantId,
                                                           LocalDate fromDate,
                                                           LocalDate toDate,
                                                           Double feeRate) {
        validateSettlementRange(fromDate, toDate);

        Merchant merchant = merchantService.getActiveMerchant(merchantId);
        double appliedFeeRate = feeRate != null ? feeRate : 0.0;
        if (appliedFeeRate < 0) {
            throw new IllegalArgumentException("feeRate must be greater than or equal to 0");
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findByMerchantIdAndStatusAndTransactionDateBetweenOrderByTransactionDateAsc(
                        merchantId,
                        "SUCCESS",
                        from,
                        to
                );

        double grossAmount = transactions.stream()
                .filter(this::affectsSettlement)
                .mapToDouble(this::signedSettlementAmount)
                .sum();

        int transactionCount = (int) transactions.stream()
                .filter(this::affectsSettlement)
                .count();

        double feeAmount = grossAmount * appliedFeeRate / 100.0;
        double netAmount = grossAmount - feeAmount;

        return new MerchantSettlementPreviewResponse(
                merchant.getMerchantId(),
                merchant.getName(),
                merchant.getResolvedSettlementAccountNumber(),
                merchant.getResolvedSettlementAccountName(),
                merchant.getResolvedSettlementBankName(),
                merchant.getResolvedSettlementAccountBalance(),
                "USD",
                fromDate,
                toDate,
                transactionCount,
                grossAmount,
                appliedFeeRate,
                feeAmount,
                netAmount
        );
    }

    private void validateSettlementRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be before or equal to toDate");
        }
    }

    private boolean affectsSettlement(Transaction tx) {
        return "CHARGE".equalsIgnoreCase(tx.getTransactionType())
                || "WITHDRAWAL".equalsIgnoreCase(tx.getTransactionType())
                || "REFUND".equalsIgnoreCase(tx.getTransactionType())
                || "REVERSAL".equalsIgnoreCase(tx.getTransactionType());
    }

    private double signedSettlementAmount(Transaction tx) {
        double amount = tx.getAmount() != null ? tx.getAmount() : 0.0;
        if ("REFUND".equalsIgnoreCase(tx.getTransactionType())
                || "REVERSAL".equalsIgnoreCase(tx.getTransactionType())) {
            return -amount;
        }
        return amount;
    }

    private Double resolveCurrentSettlementBalance(String merchantId, String settlementAccountNumber) {
        Merchant merchant = merchantService.getActiveMerchant(merchantId);
        if (settlementAccountNumber != null && settlementAccountNumber.equals(merchant.getResolvedSettlementAccountNumber())) {
            return merchant.getResolvedSettlementAccountBalance();
        }
        if (settlementAccountNumber == null || settlementAccountNumber.isBlank()) {
            return null;
        }
        return savingsAccountService.getBalance(settlementAccountNumber);
    }

    private MerchantSettlementBatchResponse toBatchResponse(MerchantSettlementBatch batch, Double settlementAccountBalance) {
        return new MerchantSettlementBatchResponse(
                batch.getId(),
                batch.getMerchantId(),
                batch.getMerchantName(),
                batch.getSettlementAccountNumber(),
                batch.getSettlementAccountName(),
                batch.getSettlementBankName(),
                settlementAccountBalance,
                batch.getCurrency(),
                batch.getFromDate(),
                batch.getToDate(),
                batch.getTransactionCount(),
                batch.getGrossAmount(),
                batch.getFeeRate(),
                batch.getFeeAmount(),
                batch.getNetAmount(),
                batch.getStatus(),
                batch.getExecutedAt(),
                batch.getExecutionReference(),
                batch.getNote(),
                batch.getCreatedAt()
        );
    }
}
