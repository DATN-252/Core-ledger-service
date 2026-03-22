package com.bkbank.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_settlement_batch_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSettlementBatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private MerchantSettlementBatch batch;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "payment_id", length = 64)
    private String paymentId;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    @Column(name = "signed_amount", nullable = false)
    private Double signedAmount;

    @Column(length = 10)
    private String currency;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 255)
    private String description;
}
