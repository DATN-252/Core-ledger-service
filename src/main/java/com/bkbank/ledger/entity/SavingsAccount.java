package com.bkbank.ledger.entity;

import com.bkbank.ledger.entity.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "savings_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SavingsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private Double balance = 0.0;

    @Column(nullable = false)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.PENDING;

    @Column(name = "lock_reason")
    private String lockReason;

    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    // Client relationship (replaces clientName)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_fk")
    private Branch branch;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Check if account has sufficient balance
     */
    public boolean hasSufficientBalance(Double amount) {
        return this.balance >= amount;
    }

    // ==================== Transaction Methods ====================

    /**
     * Withdraw money from account
     */
    public void withdraw(Double amount) {
        if (!canTransact()) {
            throw new IllegalStateException("Account cannot transact. Status: " + status +
                (lockReason != null ? ". Reason: " + lockReason : ""));
        }
        if (!hasSufficientBalance(amount)) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        this.balance -= amount;
    }

    /**
     * Deposit money to account
     */
    public void deposit(Double amount) {
        if (!canTransact()) {
            throw new IllegalStateException("Account cannot transact. Status: " + status +
                (lockReason != null ? ". Reason: " + lockReason : ""));
        }
        this.balance += amount;
    }

    /**
     * Apply a refund/reversal credit to the account.
     * Locked accounts can still receive credits, but closed/pending accounts cannot.
     */
    public void applyCardAdjustment(Double amount) {
        if (status == AccountStatus.CLOSED || status == AccountStatus.PENDING) {
            throw new IllegalStateException("Account cannot receive card adjustments. Status: " + status);
        }
        this.balance += amount;
    }

    // ==================== State Machine Methods ====================

    /**
     * Activate account (PENDING → ACTIVE)
     */
    public void activate() {
        if (status != AccountStatus.PENDING) {
            throw new IllegalStateException("Can only activate PENDING accounts. Current status: " + status);
        }
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * Lock account (ACTIVE → LOCKED)
     */
    public void lock(String reason) {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Can only lock ACTIVE accounts. Current status: " + status);
        }
        this.status = AccountStatus.LOCKED;
        this.lockReason = reason;
    }

    /**
     * Unlock account (LOCKED → ACTIVE)
     */
    public void unlock() {
        if (status != AccountStatus.LOCKED) {
            throw new IllegalStateException("Can only unlock LOCKED accounts. Current status: " + status);
        }
        this.status = AccountStatus.ACTIVE;
        this.lockReason = null;
    }

    /**
     * Close account (ACTIVE/LOCKED → CLOSED)
     * Validates that balance is zero before closing
     */
    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed");
        }
        if (status == AccountStatus.PENDING) {
            throw new IllegalStateException("Cannot close PENDING account. Activate or delete instead.");
        }
        if (balance > 0) {
            throw new IllegalStateException(
                String.format("Cannot close account with balance. Current balance: %.2f. Withdraw all funds first.", balance)
            );
        }
        this.status = AccountStatus.CLOSED;
        this.closedDate = LocalDateTime.now();
    }

    // ==================== Status Checks ====================

    /**
     * Check if account is active
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Check if account can perform transactions
     */
    public boolean canTransact() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Check if account is locked
     */
    public boolean isLocked() {
        return status == AccountStatus.LOCKED;
    }

    /**
     * Check if account is closed
     */
    public boolean isClosed() {
        return status == AccountStatus.CLOSED;
    }

    /**
     * Backward compatibility: Get client name
     * For API responses to maintain compatibility
     */
    public String getClientName() {
        return client != null ? client.getFullName() : null;
    }

    public String getBranchId() {
        return branch != null ? branch.getBranchId() : null;
    }

    public String getBranchName() {
        return branch != null ? branch.getBranchName() : null;
    }
}
