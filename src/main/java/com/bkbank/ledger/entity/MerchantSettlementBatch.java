package com.bkbank.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_settlement_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MerchantSettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "settlement_account_number", nullable = false, length = 50)
    private String settlementAccountNumber;

    @Column(name = "settlement_account_name", nullable = false)
    private String settlementAccountName;

    @Column(name = "settlement_bank_name", nullable = false)
    private String settlementBankName;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount = 0;

    @Column(name = "adjustment_count", nullable = false)
    private Integer adjustmentCount = 0;

    @Column(name = "gross_amount", nullable = false)
    private Double grossAmount = 0.0;

    @Column(name = "adjustment_amount", nullable = false)
    private Double adjustmentAmount = 0.0;

    @Column(name = "fee_rate", nullable = false)
    private Double feeRate = 0.0;

    @Column(name = "fee_amount", nullable = false)
    private Double feeAmount = 0.0;

    @Column(name = "net_amount", nullable = false)
    private Double netAmount = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "execution_reference", length = 64)
    private String executionReference;

    @Column(length = 500)
    private String note;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum SettlementStatus {
        PENDING,
        SETTLED,
        FAILED,
        CANCELLED
    }
}
