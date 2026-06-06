package com.bkbank.ledger.dto.request;

import lombok.Data;

@Data
public class StatementPaymentRequest {
    private String paymentOption;
    private Double amount;
    private String paymentSource;
    private String sourceAccountNumber;
    private String note;
}
