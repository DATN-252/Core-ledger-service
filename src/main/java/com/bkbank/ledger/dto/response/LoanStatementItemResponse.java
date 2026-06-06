package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatementItemResponse {
    private LocalDateTime transactionDate;
    private String paymentId;
    private String transactionType;
    private String merchantId;
    private String merchantName;
    private Double amount;
    private Double balanceAfter;
    private String status;
    private String responseCode;
    private String responseMessage;
}
