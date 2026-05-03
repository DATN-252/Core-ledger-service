package com.bkbank.ledger.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class LoanStatementSettingsUpdateRequest {
    private Integer billingDayOfMonth;
    private Integer paymentDueDays;
    private Double minimumPaymentRate;
    private Double minimumPaymentFloor;
    @JsonAlias("statementInterestRateMonthly")
    private Double statementInterestRateAnnual;
    private Double statementLateFeeRate;
    private Double statementLateFeeFixed;
}