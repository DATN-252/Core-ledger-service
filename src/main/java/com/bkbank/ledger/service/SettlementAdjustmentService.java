package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.response.MerchantSettlementAdjustmentResponse;
import com.bkbank.ledger.entity.MerchantSettlementAdjustment;
import com.bkbank.ledger.entity.MerchantSettlementBatch;
import com.bkbank.ledger.entity.MerchantSettlementBatchItem;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.MerchantSettlementAdjustmentRepository;
import com.bkbank.ledger.repository.MerchantSettlementBatchItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementAdjustmentService {

    private final MerchantSettlementAdjustmentRepository adjustmentRepository;
    private final MerchantSettlementBatchItemRepository batchItemRepository;

    @Transactional
    public void createPostSettlementAdjustmentIfNeeded(Transaction originalTransaction,
                                                       Transaction adjustmentTransaction,
                                                       String adjustmentType,
                                                       String reason) {
        if (originalTransaction == null || adjustmentTransaction == null || originalTransaction.getId() == null || adjustmentTransaction.getId() == null) {
            return;
        }
        if (!adjustmentRepository.existsByAdjustmentTransactionId(adjustmentTransaction.getId())) {
            MerchantSettlementBatchItem settledItem = batchItemRepository.findWithBatchByTransactionIdOrderByIdDesc(originalTransaction.getId())
                    .stream()
                    .filter(item -> item.getBatch() != null && item.getBatch().getStatus() == MerchantSettlementBatch.SettlementStatus.SETTLED)
                    .findFirst()
                    .orElse(null);

            if (settledItem == null || settledItem.getBatch() == null) {
                return;
            }

            MerchantSettlementAdjustment adjustment = new MerchantSettlementAdjustment();
            adjustment.setMerchantId(originalTransaction.getMerchantId());
            adjustment.setMerchantName(originalTransaction.getMerchantName());
            adjustment.setOriginalTransactionId(originalTransaction.getId());
            adjustment.setOriginalPaymentId(originalTransaction.getPaymentId());
            adjustment.setAdjustmentTransactionId(adjustmentTransaction.getId());
            adjustment.setOriginalBatchId(settledItem.getBatch().getId());
            adjustment.setAdjustmentType("REVERSAL".equalsIgnoreCase(adjustmentType)
                    ? MerchantSettlementAdjustment.AdjustmentType.REVERSAL_AFTER_SETTLEMENT
                    : MerchantSettlementAdjustment.AdjustmentType.REFUND_AFTER_SETTLEMENT);
            adjustment.setAmount(adjustmentTransaction.getAmount());
            adjustment.setCurrency(adjustmentTransaction.getCurrency());
            adjustment.setStatus(MerchantSettlementAdjustment.AdjustmentStatus.PENDING);
            adjustment.setReason(reason);
            adjustmentRepository.save(adjustment);
        }
    }

    public List<MerchantSettlementAdjustment> getPendingAdjustments(String merchantId) {
        return adjustmentRepository.findByMerchantIdAndStatusOrderByCreatedAtAsc(
                merchantId,
                MerchantSettlementAdjustment.AdjustmentStatus.PENDING
        );
    }

    @Transactional
    public void reserveAdjustments(Long batchId, List<MerchantSettlementAdjustment> adjustments) {
        if (adjustments.isEmpty()) {
            return;
        }
        adjustments.forEach(adjustment -> {
            adjustment.setStatus(MerchantSettlementAdjustment.AdjustmentStatus.RESERVED);
            adjustment.setReservedBatchId(batchId);
        });
        adjustmentRepository.saveAll(adjustments);
    }

    public List<MerchantSettlementAdjustment> getAdjustmentsReservedForBatch(String merchantId, Long batchId) {
        return adjustmentRepository.findByMerchantIdAndStatusInAndReservedBatchIdOrderByCreatedAtAsc(
                merchantId,
                List.of(
                        MerchantSettlementAdjustment.AdjustmentStatus.RESERVED,
                        MerchantSettlementAdjustment.AdjustmentStatus.APPLIED
                ),
                batchId
        );
    }

    @Transactional
    public void applyReservedAdjustments(String merchantId, Long batchId) {
        List<MerchantSettlementAdjustment> adjustments = adjustmentRepository.findByMerchantIdAndStatusInAndReservedBatchIdOrderByCreatedAtAsc(
                merchantId,
                List.of(MerchantSettlementAdjustment.AdjustmentStatus.RESERVED),
                batchId
        );
        adjustments.forEach(adjustment -> {
            adjustment.setStatus(MerchantSettlementAdjustment.AdjustmentStatus.APPLIED);
            adjustment.setAppliedBatchId(batchId);
        });
        adjustmentRepository.saveAll(adjustments);
    }

    public Page<MerchantSettlementAdjustmentResponse> getAdjustments(String merchantId, Pageable pageable) {
        return adjustmentRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
                .map(this::toResponse);
    }

    public MerchantSettlementAdjustmentResponse getAdjustment(String merchantId, Long adjustmentId) {
        return adjustmentRepository.findByIdAndMerchantId(adjustmentId, merchantId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Settlement adjustment not found"));
    }

    public List<MerchantSettlementAdjustmentResponse> toResponses(Collection<MerchantSettlementAdjustment> adjustments) {
        return adjustments.stream().map(this::toResponse).toList();
    }

    private MerchantSettlementAdjustmentResponse toResponse(MerchantSettlementAdjustment adjustment) {
        return new MerchantSettlementAdjustmentResponse(
                adjustment.getId(),
                adjustment.getMerchantId(),
                adjustment.getMerchantName(),
                adjustment.getOriginalTransactionId(),
                adjustment.getOriginalPaymentId(),
                adjustment.getAdjustmentTransactionId(),
                adjustment.getOriginalBatchId(),
                adjustment.getAdjustmentType(),
                adjustment.getAmount(),
                adjustment.getCurrency(),
                adjustment.getStatus(),
                adjustment.getReason(),
                adjustment.getReservedBatchId(),
                adjustment.getAppliedBatchId(),
                adjustment.getCreatedAt(),
                adjustment.getUpdatedAt()
        );
    }
}
