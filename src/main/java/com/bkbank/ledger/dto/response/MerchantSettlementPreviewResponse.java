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
    private String settlementAccountNumber;
    private String settlementAccountName;
    private String settlementBankName;
    private Double settlementAccountBalance;
    private String currency;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer transactionCount;
    private Integer adjustmentCount;
    private Double grossAmount;
    private Double adjustmentAmount;
    private Double feeRate;
    private Double feeAmount;
    private Double netAmount;
}
