package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardStatementSummaryResponse {
    private Long statementId;
    private String accountNumber;
    private LocalDate statementPeriodStart;
    private LocalDate statementPeriodEnd;
    private LocalDate billingDate;
    private LocalDate dueDate;
    private Double previousBalance;
    private Double totalCharges;
    private Double totalPayments;
    private Double minimumDue;
    private Double newBalance;
    private Double availableCredit;
    private Integer transactionCount;
    private String statementStatus;
    private Double paidAmountAfterStatement;
    private Double remainingMinimumDue;
    private Double remainingBalance;
    private LocalDateTime lastPaymentDate;
    private LocalDateTime generatedAt;
}
