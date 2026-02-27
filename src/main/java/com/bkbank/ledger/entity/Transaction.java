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
    private String transactionType; // WITHDRAWAL, DEPOSIT, CHARGE, PAYMENT

    @Column(nullable = false)
    private Double amount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    private String description;

    private Double balanceAfter; // Snapshot of balance/outstanding after transaction

    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'SUCCESS'")
    private String status = "SUCCESS"; // SUCCESS or FAILED

    public static Transaction createWithdrawal(String accountNumber, Double amount, Double balanceAfter, String merchantId, String merchantName) {
        Transaction tx = new Transaction();
        tx.accountNumber = accountNumber;
        tx.accountType = "SAVINGS";
        tx.transactionType = "WITHDRAWAL";
        tx.amount = amount;
        tx.balanceAfter = balanceAfter;
        tx.description = "Card withdrawal";
        tx.merchantId = merchantId;
        tx.merchantName = merchantName;
        tx.status = "SUCCESS";
        return tx;
    }

    public static Transaction createFailedWithdrawal(String accountNumber, Double amount, Double currentBalance, String merchantId, String merchantName, String failureReason) {
        Transaction tx = new Transaction();
        tx.accountNumber = accountNumber;
        tx.accountType = "SAVINGS";
        tx.transactionType = "WITHDRAWAL";
        tx.amount = amount;
        tx.balanceAfter = currentBalance; // Balance hasn't changed
        tx.description = "Failed withdrawal: " + failureReason;
        tx.merchantId = merchantId;
        tx.merchantName = merchantName;
        tx.status = "FAILED";
        return tx;
    }

    public static Transaction createDeposit(String accountNumber, Double amount, Double balanceAfter) {
        Transaction tx = new Transaction();
        tx.accountNumber = accountNumber;
        tx.accountType = "SAVINGS";
        tx.transactionType = "DEPOSIT";
        tx.amount = amount;
        tx.balanceAfter = balanceAfter;
        tx.description = "Deposit";
        tx.status = "SUCCESS";
        return tx;
    }

    public static Transaction createCharge(String accountNumber, Double amount, Double outstandingAfter, String merchantId, String merchantName) {
        Transaction tx = new Transaction();
        tx.accountNumber = accountNumber;
        tx.accountType = "LOAN";
        tx.transactionType = "CHARGE";
        tx.amount = amount;
        tx.balanceAfter = outstandingAfter;
        tx.description = "Credit card charge";
        tx.merchantId = merchantId;
        tx.merchantName = merchantName;
        tx.status = "SUCCESS";
        return tx;
    }

    public static Transaction createFailedCharge(String accountNumber, Double amount, Double currentOutstanding, String merchantId, String merchantName, String failureReason) {
        Transaction tx = new Transaction();
        tx.accountNumber = accountNumber;
        tx.accountType = "LOAN";
        tx.transactionType = "CHARGE";
        tx.amount = amount;
        tx.balanceAfter = currentOutstanding; // Outstanding hasn't changed
        tx.description = "Failed charge: " + failureReason;
        tx.merchantId = merchantId;
        tx.merchantName = merchantName;
        tx.status = "FAILED";
        return tx;
    }

    public static Transaction createPayment(String accountNumber, Double amount, Double outstandingAfter) {
        Transaction tx = new Transaction();
        tx.accountNumber = accountNumber;
        tx.accountType = "LOAN";
        tx.transactionType = "PAYMENT";
        tx.amount = amount;
        tx.balanceAfter = outstandingAfter;
        tx.description = "Loan payment";
        tx.status = "SUCCESS";
        return tx;
    }
}
