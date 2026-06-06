package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatementPaymentResponse {
    private String loanId;
    private LocalDate billingDate;
    private String paymentOption;
    private String paymentSource;
    private String sourceAccountNumber;
    private Double paymentAmount;
    private String currency;
    private String statementStatusBefore;
    private String statementStatusAfter;
    private Double remainingMinimumDueBefore;
    private Double remainingMinimumDueAfter;
    private Double remainingBalanceBefore;
    private Double remainingBalanceAfter;
    private Double sourceAccountBalanceAfter;
    private String paymentId;
    private LocalDateTime paidAt;
    private String note;
}
