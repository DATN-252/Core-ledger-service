package com.bkbank.ledger.dto.response;

import com.bkbank.ledger.entity.MerchantSettlementAdjustment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSettlementAdjustmentResponse {
    private Long id;
    private String merchantId;
    private String merchantName;
    private Long originalTransactionId;
    private String originalPaymentId;
    private Long adjustmentTransactionId;
    private Long originalBatchId;
    private MerchantSettlementAdjustment.AdjustmentType adjustmentType;
    private Double amount;
    private String currency;
    private MerchantSettlementAdjustment.AdjustmentStatus status;
    private String reason;
    private Long reservedBatchId;
    private Long appliedBatchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
