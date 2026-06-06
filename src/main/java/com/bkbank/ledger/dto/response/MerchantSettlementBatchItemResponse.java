package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSettlementBatchItemResponse {
    private Long id;
    private Long transactionId;
    private String paymentId;
    private String transactionType;
    private Double signedAmount;
    private String currency;
    private String accountNumber;
    private String accountType;
    private LocalDateTime transactionDate;
    private String status;
    private String description;
}
