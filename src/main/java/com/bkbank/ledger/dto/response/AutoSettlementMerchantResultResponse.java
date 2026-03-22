package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoSettlementMerchantResultResponse {
    private String merchantId;
    private String merchantName;
    private String status;
    private String message;
    private Long batchId;
    private String executionReference;
}
