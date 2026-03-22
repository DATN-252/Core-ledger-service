package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoSettlementRunResponse {
    private LocalDate settlementDate;
    private Double feeRate;
    private Boolean autoExecuted;
    private Integer generatedCount;
    private Integer executedCount;
    private Integer skippedCount;
    private Integer failedCount;
    private LocalDateTime runAt;
    private List<AutoSettlementMerchantResultResponse> results;
}
