package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.response.AutoSettlementMerchantResultResponse;
import com.bkbank.ledger.dto.response.AutoSettlementRunResponse;
import com.bkbank.ledger.dto.response.MerchantSettlementAdjustmentResponse;
import com.bkbank.ledger.dto.response.MerchantSettlementBatchItemResponse;
import com.bkbank.ledger.dto.response.MerchantSettlementBatchResponse;
import com.bkbank.ledger.dto.response.MerchantSettlementPreviewResponse;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.entity.MerchantSettlementAdjustment;
import com.bkbank.ledger.entity.MerchantSettlementBatch;
import com.bkbank.ledger.entity.MerchantSettlementBatchItem;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.MerchantRepository;
import com.bkbank.ledger.repository.MerchantSettlementBatchItemRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final MerchantService merchantService;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final MerchantSettlementBatchRepository merchantSettlementBatchRepository;
    private final MerchantSettlementBatchItemRepository merchantSettlementBatchItemRepository;
    private final SavingsAccountService savingsAccountService;
    private final SettlementAdjustmentService settlementAdjustmentService;

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
            if (preview.getAdjustmentCount() == null || preview.getAdjustmentCount() <= 0) {
                throw new IllegalArgumentException("Không có giao dịch đủ điều kiện để quyết toán");
            }
        }

        List<Transaction> eligibleTransactions = findEligibleTransactions(merchantId, fromDate, toDate);
        List<MerchantSettlementAdjustment> pendingAdjustments = settlementAdjustmentService.getPendingAdjustments(merchantId);

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
            throw new IllegalArgumentException("Đã có batch settlement cho merchant và khoảng thời gian này");
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
        batch.setAdjustmentCount(preview.getAdjustmentCount());
        batch.setGrossAmount(preview.getGrossAmount());
        batch.setAdjustmentAmount(preview.getAdjustmentAmount());
        batch.setFeeRate(preview.getFeeRate());
        batch.setFeeAmount(preview.getFeeAmount());
        batch.setNetAmount(preview.getNetAmount());
        batch.setStatus(MerchantSettlementBatch.SettlementStatus.PENDING);
        batch.setNote(note);
        batch.setExecutionReference("SET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());

        batch = merchantSettlementBatchRepository.save(batch);
        merchantSettlementBatchItemRepository.saveAll(buildBatchItems(batch, eligibleTransactions));
        settlementAdjustmentService.reserveAdjustments(batch.getId(), pendingAdjustments);

        return toBatchResponse(batch, preview.getSettlementAccountBalance(), true);
    }

    @Transactional
    public MerchantSettlementBatchResponse executeSettlementBatch(String merchantId,
                                                                  Long batchId,
                                                                  String note) {
        MerchantSettlementBatch batch = merchantSettlementBatchRepository.findByIdAndMerchantId(batchId, merchantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy batch settlement"));

        if (batch.getStatus() != MerchantSettlementBatch.SettlementStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể thực hiện batch PENDING");
        }

        if (batch.getNetAmount() == null || batch.getNetAmount() <= 0) {
            throw new IllegalArgumentException("Settlement batch has no positive net amount to deposit");
        }

        Merchant merchant = merchantService.getActiveMerchant(merchantId);
        String settlementAccountNumber = merchant.getResolvedSettlementAccountNumber();
        if (settlementAccountNumber == null || settlementAccountNumber.isBlank()) {
            throw new IllegalArgumentException("Tài khoản settlement của merchant chưa được cấu hình"); 
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
        settlementAdjustmentService.applyReservedAdjustments(merchantId, batch.getId());

        return toBatchResponse(batch, settlementAccount.getBalance(), true);
    }

    public Page<MerchantSettlementBatchResponse> getSettlementBatches(String merchantId, Pageable pageable) {
        merchantService.getActiveMerchant(merchantId);
        return merchantSettlementBatchRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
                .map(batch -> toBatchResponse(batch, resolveCurrentSettlementBalance(merchantId, batch.getSettlementAccountNumber()), false));
    }

    public MerchantSettlementBatchResponse getSettlementBatch(String merchantId, Long batchId) {
        MerchantSettlementBatch batch = merchantSettlementBatchRepository.findByIdAndMerchantId(batchId, merchantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy batch settlement"));    
        return toBatchResponse(batch, resolveCurrentSettlementBalance(merchantId, batch.getSettlementAccountNumber()), true);
    }

    @Transactional
    public AutoSettlementRunResponse runAutomaticSettlement(LocalDate settlementDate,
                                                            Double feeRate,
                                                            boolean executeBatches) {
        LocalDate effectiveDate = settlementDate != null ? settlementDate : LocalDate.now().minusDays(1);
        double appliedFeeRate = feeRate != null ? feeRate : 0.0;

        List<Merchant> merchants = merchantRepository.findByStatusOrderByMerchantIdAsc(Merchant.MerchantStatus.ACTIVE);
        List<AutoSettlementMerchantResultResponse> results = new ArrayList<>();
        int generatedCount = 0;
        int executedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Merchant merchant : merchants) {
            try {
                List<Transaction> eligibleTransactions = findEligibleTransactions(
                        merchant.getMerchantId(),
                        effectiveDate,
                        effectiveDate
                );
                if (eligibleTransactions.isEmpty()) {
                    skippedCount++;
                    results.add(new AutoSettlementMerchantResultResponse(
                            merchant.getMerchantId(),
                            merchant.getName(),
                            "SKIPPED",
                            "Không có giao dịch đủ điều kiện",
                            null,
                            null
                    ));
                    continue;
                }

                boolean duplicateExists = merchantSettlementBatchRepository.existsByMerchantIdAndFromDateAndToDateAndStatusIn(
                        merchant.getMerchantId(),
                        effectiveDate,
                        effectiveDate,
                        List.of(
                                MerchantSettlementBatch.SettlementStatus.PENDING,
                                MerchantSettlementBatch.SettlementStatus.SETTLED
                        )
                );
                if (duplicateExists) {
                    skippedCount++;
                    results.add(new AutoSettlementMerchantResultResponse(
                            merchant.getMerchantId(),
                            merchant.getName(),
                            "SKIPPED",
                            "Đã có settlement batch cho merchant và ngày này",
                            null,
                            null
                    ));
                    continue;
                }

                MerchantSettlementBatchResponse generatedBatch = generateSettlementBatch(
                        merchant.getMerchantId(),
                        effectiveDate,
                        effectiveDate,
                        appliedFeeRate,
                        "T+1 settlement tự động cho " + effectiveDate
                );
                generatedCount++;

                MerchantSettlementBatchResponse finalBatch = generatedBatch;
                String status = "GENERATED";
                String message = "Đã tạo batch";

                if (executeBatches) {
                    finalBatch = executeSettlementBatch(
                            merchant.getMerchantId(),
                            generatedBatch.getId(),
                            "AUTO T+1 settlement executed for " + effectiveDate
                    );
                    executedCount++;
                    status = "EXECUTED";
                    message = "Đã thực hiện batch";
                }

                results.add(new AutoSettlementMerchantResultResponse(
                        merchant.getMerchantId(),
                        merchant.getName(),
                        status,
                        message,
                        finalBatch.getId(),
                        finalBatch.getExecutionReference()
                ));
            } catch (Exception ex) {
                failedCount++;
                results.add(new AutoSettlementMerchantResultResponse(
                        merchant.getMerchantId(),
                        merchant.getName(),
                        "FAILED",
                        ex.getMessage(),
                        null,
                        null
                ));
            }
        }

        return new AutoSettlementRunResponse(
                effectiveDate,
                appliedFeeRate,
                executeBatches,
                generatedCount,
                executedCount,
                skippedCount,
                failedCount,
                LocalDateTime.now(),
                results
        );
    }

    private MerchantSettlementPreviewResponse buildPreview(String merchantId,
                                                           LocalDate fromDate,
                                                           LocalDate toDate,
                                                           Double feeRate) {
        validateSettlementRange(fromDate, toDate);

        Merchant merchant = merchantService.getActiveMerchant(merchantId);
        double appliedFeeRate = feeRate != null ? feeRate : 0.0;
        if (appliedFeeRate < 0) {
            throw new IllegalArgumentException("feeRate phải lớn hơn hoặc bằng 0");
        }

        List<Transaction> transactions = findEligibleTransactions(merchantId, fromDate, toDate);
        List<MerchantSettlementAdjustment> pendingAdjustments = settlementAdjustmentService.getPendingAdjustments(merchantId);
        double adjustmentAmount = pendingAdjustments.stream()
                .mapToDouble(adjustment -> adjustment.getAmount() != null ? adjustment.getAmount() : 0.0)
                .sum();

        double grossAmount = transactions.stream()
                .mapToDouble(this::signedSettlementAmount)
                .sum();

        int transactionCount = transactions.size();
        int adjustmentCount = pendingAdjustments.size();
        double adjustedGrossAmount = grossAmount - adjustmentAmount;

        double feeAmount = adjustedGrossAmount * appliedFeeRate / 100.0;
        double netAmount = adjustedGrossAmount - feeAmount;

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
                adjustmentCount,
                grossAmount,
                adjustmentAmount,
                appliedFeeRate,
                feeAmount,
                netAmount
        );
    }

    private List<Transaction> findEligibleTransactions(String merchantId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        return transactionRepository
                .findByMerchantIdAndStatusAndTransactionDateBetweenOrderByTransactionDateAsc(
                        merchantId,
                        "SUCCESS",
                        from,
                        to
                )
                .stream()
                .filter(this::affectsSettlement)
                .toList();
    }

    private void validateSettlementRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate và toDate là bắt buộc");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate phải trước hoặc bằng toDate");
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

    private List<MerchantSettlementBatchItem> buildBatchItems(MerchantSettlementBatch batch, List<Transaction> transactions) {
        return transactions.stream()
                .map(tx -> {
                    MerchantSettlementBatchItem item = new MerchantSettlementBatchItem();
                    item.setBatch(batch);
                    item.setTransactionId(tx.getId());
                    item.setPaymentId(tx.getPaymentId());
                    item.setTransactionType(tx.getTransactionType());
                    item.setSignedAmount(signedSettlementAmount(tx));
                    item.setCurrency(tx.getCurrency());
                    item.setAccountNumber(tx.getAccountNumber());
                    item.setAccountType(tx.getAccountType());
                    item.setTransactionDate(tx.getTransactionDate());
                    item.setStatus(tx.getStatus());
                    item.setDescription(tx.getDescription());
                    return item;
                })
                .toList();
    }

    private MerchantSettlementBatchResponse toBatchResponse(MerchantSettlementBatch batch,
                                                            Double settlementAccountBalance,
                                                            boolean includeItems) {
        List<MerchantSettlementBatchItemResponse> items = includeItems
                ? merchantSettlementBatchItemRepository.findByBatchIdOrderByTransactionDateAsc(batch.getId()).stream()
                .map(item -> new MerchantSettlementBatchItemResponse(
                        item.getId(),
                        item.getTransactionId(),
                        item.getPaymentId(),
                        item.getTransactionType(),
                        item.getSignedAmount(),
                        item.getCurrency(),
                        item.getAccountNumber(),
                        item.getAccountType(),
                        item.getTransactionDate(),
                        item.getStatus(),
                        item.getDescription()
                ))
                .toList()
                : null;

        List<MerchantSettlementAdjustmentResponse> adjustments = includeItems
                ? settlementAdjustmentService.toResponses(
                settlementAdjustmentService.getAdjustmentsReservedForBatch(batch.getMerchantId(), batch.getId())
        )
                : null;

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
                batch.getAdjustmentCount(),
                batch.getGrossAmount(),
                batch.getAdjustmentAmount(),
                batch.getFeeRate(),
                batch.getFeeAmount(),
                batch.getNetAmount(),
                batch.getStatus(),
                batch.getExecutedAt(),
                batch.getExecutionReference(),
                batch.getNote(),
                batch.getCreatedAt(),
                items,
                adjustments
        );
    }
}
