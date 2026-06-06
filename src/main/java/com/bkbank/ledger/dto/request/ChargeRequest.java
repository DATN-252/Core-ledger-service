package com.bkbank.ledger.dto.request;

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

    // Location Information
    private String location;
    private Double latitude;
    private Double longitude;

    // Standard transaction references
    private String paymentId;
    private String idempotencyKey;
    private String originalTransactionId;
    private String channel;
    private String authCode;
    private String stan;
    private String rrn;
    private String externalReference;
    private String responseCode;
    private String responseMessage;
    private String paymentNote;
}
