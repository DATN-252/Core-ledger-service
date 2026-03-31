package com.bkbank.ledger.dto.request;

import lombok.Data;

@Data
public class FraudAlertNotificationRequest {
    private Long fraudAlertId;
    private String accountId;
    private String accountType;
    private String paymentId;
    private String maskedPan;
    private Double amount;
    private String currency;
    private String merchantName;
    private String riskLevel;
}
