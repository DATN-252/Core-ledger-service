package com.bkbank.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest {
    private String locale = "en";
    private String dateFormat = "dd MMMM yyyy";
    private String transactionDate;
    private Double transactionAmount;
    
    // Merchant Information
    private String merchantId;
    private String merchantName;
}
