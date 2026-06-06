package com.bkbank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentErrorDetail {
    private String errorCode;
    private String responseCode;
    private String responseMessage;
    private String errorTitle;
    private String errorHint;
    private int httpStatus;
    private boolean retryable;
    private String paymentId;
    private String idempotencyKey;
}
