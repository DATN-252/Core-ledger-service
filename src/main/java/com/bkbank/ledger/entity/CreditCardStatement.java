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
@Table(name = "credit_card_statements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_credit_card_statements_account_billing", columnNames = {"account_number", "billing_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CreditCardStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "statement_period_start", nullable = false)
    private LocalDate statementPeriodStart;

    @Column(name = "statement_period_end", nullable = false)
    private LocalDate statementPeriodEnd;

    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "previous_balance", nullable = false)
    private Double previousBalance;

    @Column(name = "total_charges", nullable = false)
    private Double totalCharges;

    @Column(name = "total_payments", nullable = false)
    private Double totalPayments;

    @Column(name = "minimum_due", nullable = false)
    private Double minimumDue;

    @Column(name = "current_minimum_due", nullable = false)
    private Double currentMinimumDue = 0.0;

    @Column(name = "past_due_minimum", nullable = false)
    private Double pastDueMinimum = 0.0;

    @Column(name = "total_minimum_due_now", nullable = false)
    private Double totalMinimumDueNow = 0.0;

    @Column(name = "new_balance", nullable = false)
    private Double newBalance;

    @Column(name = "grace_period_eligible", nullable = false)
    private Boolean gracePeriodEligible = false;

    @Column(name = "interest_rate_annual", nullable = false)
    private Double interestRateAnnual = 30.0;

    @Column(name = "interest_charged", nullable = false)
    private Double interestCharged = 0.0;

    @Column(name = "interest_applied_at")
    private LocalDateTime interestAppliedAt;

    @Column(name = "late_fee_rate", nullable = false)
    private Double lateFeeRate = 4.0;

    @Column(name = "late_fee_fixed", nullable = false)
    private Double lateFeeFixed = 15.0;

    @Column(name = "late_fee_charged", nullable = false)
    private Double lateFeeCharged = 0.0;

    @Column(name = "late_fee_applied_at")
    private LocalDateTime lateFeeAppliedAt;

    @Column(name = "available_credit_at_billing", nullable = false)
    private Double availableCreditAtBilling;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @Column(name = "statement_status", nullable = false, length = 30)
    private String statementStatus = "OPEN";

    @Column(name = "paid_amount_after_statement", nullable = false)
    private Double paidAmountAfterStatement = 0.0;

    @Column(name = "remaining_minimum_due", nullable = false)
    private Double remainingMinimumDue = 0.0;

    @Column(name = "remaining_balance", nullable = false)
    private Double remainingBalance = 0.0;

    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
