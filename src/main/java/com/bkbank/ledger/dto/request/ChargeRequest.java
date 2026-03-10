package com.bkbank.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargeRequest {
    private Integer chargeId; // Ignored for compatibility
    private Double amount;
    private String dueDate;
    private String locale = "en";
    private String dateFormat = "dd MMMM yyyy";
    private String cardNetwork;
    
    // Merchant Information
    private String merchantId;
    private String merchantName;
}
