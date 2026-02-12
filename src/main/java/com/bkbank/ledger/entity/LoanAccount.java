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

    @Column(nullable = false)
    private Double principalOutstanding = 0.0; // Current debt

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, CLOSED

    // Client relationship (replaces clientName)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

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
        if (!hasSufficientCredit(amount)) {
            throw new IllegalArgumentException("Credit limit exceeded");
        }
        this.principalOutstanding += amount;
    }

    /**
     * Make payment (reduce outstanding)
     */
    public void makePayment(Double amount) {
        if (amount > this.principalOutstanding) {
            throw new IllegalArgumentException("Payment exceeds outstanding balance");
        }
        this.principalOutstanding -= amount;
    }

    /**
     * Check if account is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    /**
     * Backward compatibility: Get client name
     * For API responses to maintain compatibility
     */
    public String getClientName() {
        return client != null ? client.getFullName() : null;
    }
}
