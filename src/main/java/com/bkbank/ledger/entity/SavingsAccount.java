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

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, LOCKED, CLOSED

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
     * Check if account has sufficient balance
     */
    public boolean hasSufficientBalance(Double amount) {
        return this.balance >= amount;
    }

    /**
     * Withdraw money from account
     */
    public void withdraw(Double amount) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        this.balance -= amount;
    }

    /**
     * Deposit money to account
     */
    public void deposit(Double amount) {
        this.balance += amount;
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
