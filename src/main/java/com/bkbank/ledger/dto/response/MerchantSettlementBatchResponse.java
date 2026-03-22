package com.bkbank.ledger.dto.response;

import com.bkbank.ledger.entity.MerchantSettlementBatch;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSettlementBatchResponse {
    private Long id;
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
    private Double grossAmount;
    private Double feeRate;
    private Double feeAmount;
    private Double netAmount;
    private MerchantSettlementBatch.SettlementStatus status;
    private LocalDateTime executedAt;
    private String executionReference;
    private String note;
    private LocalDateTime createdAt;
    private List<MerchantSettlementBatchItemResponse> items;
}
