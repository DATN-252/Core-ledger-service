package com.bkbank.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payment Allocation Log - Audit trail để theo dõi waterfall allocation của
 * payments
 * 
 * Ví dụ:
 * Payment 1,000,000 VND:
 * → Late Fee: 15,000
 * → Past Due Minimum: 400,000
 * → Current Interest: 50,000
 * → Current Balance: 535,000
 */
@Entity
@Table(name = "payment_allocation_logs", indexes = {
        @Index(name = "idx_account_billing", columnList = "account_number,statement_billing_date"),
        @Index(name = "idx_account_payment_date", columnList = "account_number,payment_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private String paymentTransactionId;
    private LocalDate statementBillingDate;

    private LocalDate paymentDate;
    private double paymentAmount;

    // Waterfall allocation breakdown
    private double allocatedToLateFee;
    private double allocatedToPastDueMinimum;
    private double allocatedToCurrentInterest;
    private double allocatedToCurrentBalance;

    // Remaining after allocation
    private double remainingBalance;
    private double remainingMinimumDue;

    // Audit fields
    private LocalDateTime allocationTime;
    private String allocatedBy; // SYSTEM or USER_ID

    @Column(columnDefinition = "TEXT")
    private String allocationDetails; // JSON or formatted text

    private LocalDateTime createdAt;

    @Version
    private Long version;
}
