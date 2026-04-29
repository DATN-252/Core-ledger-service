package com.bkbank.ledger.dto.request;

import lombok.Data;

@Data
public class LoanStatementSettingsUpdateRequest {
    private Integer billingDayOfMonth;
    private Integer paymentDueDays;
    private Double minimumPaymentRate;
    private Double minimumPaymentFloor;
    private Double statementInterestRateMonthly;
    private Double statementLateFeeFixed;
}
