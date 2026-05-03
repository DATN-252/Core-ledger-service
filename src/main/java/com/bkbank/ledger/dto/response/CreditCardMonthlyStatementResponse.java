package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardMonthlyStatementResponse {
    private Long statementId;
    private LocalDateTime generatedAt;
    private String accountNumber;
    private String currency;
    private LocalDate statementPeriodStart;
    private LocalDate statementPeriodEnd;
    private String statementPeriod;
    private LocalDate billingDate;
    private LocalDate dueDate;
    private Double creditLimit;
    private Double previousBalance;
    private Double totalCharges;
    private Double totalPayments;
    private Double minimumDue;
    private Double currentMinimumDue;
    private Double pastDueMinimum;
    private Double totalMinimumDueNow;
    private Double remainingCurrentMinimumDue;
    private Double remainingPastDueMinimum;
    private Double newBalance;
    private Boolean gracePeriodEligible;
    private Double interestRateAnnual;
    private Double interestCharged;
    private LocalDateTime interestAppliedAt;
    private Double lateFeeRate;
    private Double lateFeeFixed;
    private Double lateFeeCharged;
    private LocalDateTime lateFeeAppliedAt;
    private Double availableCredit;
    private Integer transactionCount;
    private Integer billingDayOfMonth;
    private Integer paymentDueDays;
    private Double minimumPaymentRate;
    private Double minimumPaymentFloor;
    private String statementStatus;
    private Double paidAmountAfterStatement;
    private Double remainingMinimumDue;
    private Double remainingBalance;
    private LocalDateTime lastPaymentDate;
    private List<LoanStatementItemResponse> items;
    private List<LoanStatementItemResponse> postStatementItems;
}
