package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSettlementPreviewResponse {
    private String merchantId;
    private String merchantName;
    private String currency;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer transactionCount;
    private Double grossAmount;
    private Double feeRate;
    private Double feeAmount;
    private Double netAmount;
}
