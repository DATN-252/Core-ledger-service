package com.bkbank.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_settlement_adjustments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MerchantSettlementAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "original_transaction_id", nullable = false)
    private Long originalTransactionId;

    @Column(name = "original_payment_id", length = 64)
    private String originalPaymentId;

    @Column(name = "adjustment_transaction_id", nullable = false)
    private Long adjustmentTransactionId;

    @Column(name = "original_batch_id", nullable = false)
    private Long originalBatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 40)
    private AdjustmentType adjustmentType;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdjustmentStatus status = AdjustmentStatus.PENDING;

    @Column(length = 255)
    private String reason;

    @Column(name = "reserved_batch_id")
    private Long reservedBatchId;

    @Column(name = "applied_batch_id")
    private Long appliedBatchId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum AdjustmentType {
        REFUND_AFTER_SETTLEMENT,
        REVERSAL_AFTER_SETTLEMENT
    }

    public enum AdjustmentStatus {
        PENDING,
        RESERVED,
        APPLIED,
        CANCELLED
    }
}
