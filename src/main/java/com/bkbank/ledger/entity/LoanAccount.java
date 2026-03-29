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
@Table(name = "loan_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LoanAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private Double principal; // Credit limit

    @Column(name = "principal_outstanding")
    private Double principalOutstanding = 0.0; // Current debt

    @Column(name = "currency", nullable = false)
    private String currency = "USD";

    @Column(name = "billing_day_of_month", nullable = false)
    private Integer billingDayOfMonth = 25;

    @Column(name = "payment_due_days", nullable = false)
    private Integer paymentDueDays = 20;

    @Column(name = "minimum_payment_rate", nullable = false)
    private Double minimumPaymentRate = 5.0;

    @Column(name = "minimum_payment_floor", nullable = false)
    private Double minimumPaymentFloor = 10.0;

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
     * Validates that outstanding balance is zero before closing
     */
    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed");
        }
        if (status == AccountStatus.PENDING) {
            throw new IllegalStateException("Cannot close PENDING account. Activate or delete instead.");
        }
        if (principalOutstanding > 0) {
            throw new IllegalStateException(
                String.format("Cannot close account with outstanding debt. Current outstanding: %.2f. Pay off debt first.", principalOutstanding)
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
     * Check if account can add charges
     */
    public boolean canAddCharge() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Check if account can make payments
     * Note: LOCKED accounts can still receive payments
     */
    public boolean canMakePayment() {
        return status == AccountStatus.ACTIVE || status == AccountStatus.LOCKED;
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

    // ==================== Business Methods ====================

    /**
     * Get available credit (limit - outstanding)
     */
    public Double getAvailableCredit() {
        return principal - principalOutstanding;
    }

    /**
     * Check if has sufficient credit for transaction
     */
    public boolean hasSufficientCredit(Double amount) {
        return getAvailableCredit() >= amount;
    }

    /**
     * Add charge to loan (increase outstanding)
     */
    public void addCharge(Double amount) {
        if (!canAddCharge()) {
            throw new IllegalStateException("Account cannot add charges. Status: " + status +
                (lockReason != null ? ". Reason: " + lockReason : ""));
        }
        if (!hasSufficientCredit(amount)) {
            throw new IllegalArgumentException("Credit limit exceeded");
        }
        this.principalOutstanding += amount;
    }

    /**
     * Make payment (reduce outstanding)
     */
    public void makePayment(Double amount) {
        if (!canMakePayment()) {
            throw new IllegalStateException("Account cannot make payments. Status: " + status);
        }
        if (amount > this.principalOutstanding) {
            throw new IllegalArgumentException("Payment exceeds outstanding balance");
        }
        this.principalOutstanding -= amount;
    }

    /**
     * Apply a refund/reversal against an existing card charge.
     * Locked accounts can still receive credits, but closed/pending accounts cannot.
     */
    public void applyCardAdjustment(Double amount) {
        makePayment(amount);
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
