package com.bkbank.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountType; // SAVINGS or LOAN

    @Column(nullable = false)
    private String transactionType; // WITHDRAWAL, DEPOSIT, CHARGE, PAYMENT, REVERSAL, REFUND

    @Column(nullable = false)
    private Double amount;

    @Column(length = 10)
    private String currency;

    @Column(name = "payment_id", nullable = false, unique = true, length = 64)
    private String paymentId;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "original_transaction_id", length = 64)
    private String originalTransactionId;

    @Column(length = 20)
    private String channel;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    private String description;

    private Double balanceAfter; // Snapshot of balance/outstanding after transaction

    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "merchant_name")
    private String merchantName;

    private String location;
    private Double latitude;
    private Double longitude;

    @Column(name = "card_network")
    private String cardNetwork;

    @Column(name = "auth_code", length = 20)
    private String authCode;

    @Column(length = 20)
    private String stan;

    @Column(length = 30)
    private String rrn;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "response_message", length = 255)
    private String responseMessage;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'SUCCESS'")
    private String status = "SUCCESS"; // SUCCESS, FAILED, REVERSED, REFUNDED

    public static Transaction createWithdrawal(String accountNumber, Double amount, String currency, Double balanceAfter, String merchantId, String merchantName, String location, Double latitude, Double longitude) {
        Transaction tx = new Transaction();
        initializeDefaults(tx);
        tx.accountNumber = accountNumber;
        tx.accountType = "SAVINGS";
        tx.transactionType = "WITHDRAWAL";
        tx.amount = amount;
        tx.currency = currency;
        tx.balanceAfter = balanceAfter;
        tx.description = "Card withdrawal";
        tx.merchantId = merchantId;
        tx.merchantName = merchantName;
        tx.location = location;
        tx.latitude = latitude;
        tx.longitude = longitude;
        tx.cardNetwork = null;
        tx.status = "SUCCESS";
        tx.responseCode = "00";
        tx.responseMessage = "Approved";
        return tx;
    }

    public static Transaction createFailedWithdrawal(String accountNumber, Double amount, String currency, Double currentBalance, String merchantId, String merchantName, String location, Double latitude, Double longitude, String failureReason) {
        Transaction tx = new Transaction();
        initializeDefaults(tx);
        tx.accountNumber = accountNumber;
        tx.accountType = "SAVINGS";
        tx.transactionType = "WITHDRAWAL";
        tx.amount = amount;
        tx.currency = currency;
        tx.balanceAfter = currentBalance; // Balance hasn't changed
        tx.description = "Failed withdrawal: " + failureReason;
        tx.merchantId = merchantId;
        tx.merchantName = merchantName;
        tx.location = location;
        tx.latitude = latitude;
        tx.longitude = longitude;
        tx.cardNetwork = null;
        tx.status = "FAILED";
        tx.responseCode = "96";
        tx.responseMessage = failureReason;
        return tx;
    }

    public static Transaction createDeposit(String accountNumber, Double amount, String currency, Double balanceAfter) {
        Transaction tx = new Transaction();
        initializeDefaults(tx);
        tx.accountNumber = accountNumber;
        tx.accountType = "SAVINGS";
        tx.transactionType = "DEPOSIT";
        tx.amount = amount;
        tx.currency = currency;
        tx.balanceAfter = balanceAfter;
        tx.description = "Deposit";
        tx.status = "SUCCESS";
        tx.responseCode = "00";
        tx.responseMessage = "Approved";
        return tx;
    }

    public static Transaction createCharge(String accountNumber, Double amount, String currency, Double outstandingAfter, String merchantId, String merchantName, String location, Double latitude, Double longitude) {
        Transaction tx = new Transaction();
        initializeDefaults(tx);
        tx.accountNumber = accountNumber;
        tx.accountType = "LOAN";
        tx.transactionType = "CHARGE";
        tx.amount = amount;
        tx.currency = currency;
        tx.balanceAfter = outstandingAfter;
        tx.description = "Credit card charge";
        tx.merchantId = merchantId;
        tx.merchantName = merchantName;
        tx.location = location;
        tx.latitude = latitude;
        tx.longitude = longitude;
        tx.cardNetwork = null; // Will be set externally if needed
        tx.status = "SUCCESS";
        tx.responseCode = "00";
        tx.responseMessage = "Approved";
        return tx;
    }

    public static Transaction createFailedCharge(String accountNumber, Double amount, String currency, Double currentOutstanding, String merchantId, String merchantName, String location, Double latitude, Double longitude, String failureReason) {
        Transaction tx = new Transaction();
        initializeDefaults(tx);
        tx.accountNumber = accountNumber;
        tx.accountType = "LOAN";
        tx.transactionType = "CHARGE";
        tx.amount = amount;
        tx.currency = currency;
        tx.balanceAfter = currentOutstanding; // Outstanding hasn't changed
        tx.description = "Failed charge: " + failureReason;
        tx.merchantId = merchantId;
        tx.merchantName = merchantName;
        tx.location = location;
        tx.latitude = latitude;
        tx.longitude = longitude;
        tx.cardNetwork = null; // Will be set externally if needed
        tx.status = "FAILED";
        tx.responseCode = "96";
        tx.responseMessage = failureReason;
        return tx;
    }

    public static Transaction createPayment(String accountNumber, Double amount, String currency, Double outstandingAfter) {
        Transaction tx = new Transaction();
        initializeDefaults(tx);
        tx.accountNumber = accountNumber;
        tx.accountType = "LOAN";
        tx.transactionType = "PAYMENT";
        tx.amount = amount;
        tx.currency = currency;
        tx.balanceAfter = outstandingAfter;
        tx.description = "Loan payment";
        tx.status = "SUCCESS";
        tx.responseCode = "00";
        tx.responseMessage = "Approved";
        return tx;
    }

    public static Transaction createRefund(Transaction originalTransaction, Double balanceAfter, String reason) {
        return createAdjustment(originalTransaction, balanceAfter, "REFUND", reason);
    }

    public static Transaction createReversal(Transaction originalTransaction, Double balanceAfter, String reason) {
        return createAdjustment(originalTransaction, balanceAfter, "REVERSAL", reason);
    }

    public void applyReferenceData(String paymentId,
                                   String idempotencyKey,
                                   String originalTransactionId,
                                   String channel,
                                   String authCode,
                                   String stan,
                                   String rrn,
                                   String externalReference,
                                   String responseCode,
                                   String responseMessage) {
        if (paymentId != null && !paymentId.isBlank()) {
            this.paymentId = paymentId;
        }
        this.idempotencyKey = idempotencyKey;
        this.originalTransactionId = originalTransactionId;
        if (channel != null && !channel.isBlank()) {
            this.channel = channel;
        }
        this.authCode = authCode;
        this.stan = stan;
        this.rrn = rrn;
        this.externalReference = externalReference;
        if (responseCode != null && !responseCode.isBlank()) {
            this.responseCode = responseCode;
        }
        if (responseMessage != null && !responseMessage.isBlank()) {
            this.responseMessage = responseMessage;
        }
    }

    private static void initializeDefaults(Transaction tx) {
        tx.paymentId = generatePaymentId();
        tx.channel = "SYSTEM";
    }

    private static Transaction createAdjustment(Transaction originalTransaction,
                                                Double balanceAfter,
                                                String adjustmentType,
                                                String reason) {
        Transaction tx = new Transaction();
        initializeDefaults(tx);
        tx.accountNumber = originalTransaction.accountNumber;
        tx.accountType = originalTransaction.accountType;
        tx.transactionType = adjustmentType;
        tx.amount = originalTransaction.amount;
        tx.currency = originalTransaction.currency;
        tx.balanceAfter = balanceAfter;
        tx.description = adjustmentType + " for " + originalTransaction.paymentId
                + (reason != null && !reason.isBlank() ? " - " + reason : "");
        tx.merchantId = originalTransaction.merchantId;
        tx.merchantName = originalTransaction.merchantName;
        tx.location = originalTransaction.location;
        tx.latitude = originalTransaction.latitude;
        tx.longitude = originalTransaction.longitude;
        tx.cardNetwork = originalTransaction.cardNetwork;
        tx.status = "SUCCESS";
        tx.responseCode = "00";
        tx.responseMessage = reason != null && !reason.isBlank() ? reason : "Approved";
        tx.originalTransactionId = originalTransaction.paymentId;
        return tx;
    }

    private static String generatePaymentId() {
        return "PAY-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }
}
