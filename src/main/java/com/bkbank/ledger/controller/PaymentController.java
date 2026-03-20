package com.bkbank.ledger.controller;

import com.bkbank.ledger.client.CmsClient;
import com.bkbank.ledger.dto.response.ApiResponse;
import com.bkbank.ledger.dto.response.PaymentErrorDetail;
import com.bkbank.ledger.dto.request.PaymentAdjustmentRequest;
import com.bkbank.ledger.dto.request.PaymentRequest;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.service.LoanAccountService;
import com.bkbank.ledger.service.MerchantService;
import com.bkbank.ledger.service.PaymentAdjustmentService;
import com.bkbank.ledger.service.SavingsAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final CmsClient cmsClient;
    private final MerchantService merchantService;
    private final PaymentAdjustmentService paymentAdjustmentService;
    private final SavingsAccountService savingsAccountService;
    private final LoanAccountService loanAccountService;
    private static final String DEFAULT_BANK_NAME = "BKBank Merchant Network";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/preview")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewPayment(@RequestBody PaymentRequest request) {
        String merchantId = request.getMerchantId();
        if (merchantId == null || merchantId.trim().isEmpty()) {
            return errorResponse(HttpStatus.BAD_REQUEST,
                    "merchantId or recipientAccount is required",
                    "INVALID_REQUEST",
                    null,
                    false,
                    null,
                    null);
        }

        try {
            Merchant merchant = resolveMerchant(merchantId, null, null);
            LocalDateTime now = LocalDateTime.now();
            Double amount = request.getAmount();
            double fee = 0.0;
            String inferredNetwork = firstNonBlank(request.getCardNetwork(), inferCardNetwork(request.getCardNumber()));
            String resolvedCardType = firstNonBlank(request.getCardType(), resolveCardTypeFromNetwork(inferredNetwork));

            Map<String, Object> previewData = new HashMap<>();
            previewData.put("merchantId", merchantId);
            previewData.put("merchantName", merchant.getName());
            previewData.put("recipientAccount", merchantId);
            previewData.put("recipientName", merchant.getName());
            previewData.put("bankName", DEFAULT_BANK_NAME);
            previewData.put("amount", amount);
            previewData.put("fee", 0);
            previewData.put("totalAmount", amount != null ? amount + fee : null);
            previewData.put("currency", firstNonBlank(request.getCurrency(), "VND"));
            previewData.put("status", "VALID");
            previewData.put("cardType", resolvedCardType);
            previewData.put("cardNetwork", inferredNetwork);
            previewData.put("maskedCardNumber", maskCardNumber(request.getCardNumber()));
            previewData.put("cardholderName", request.getCardholderName());
            previewData.put("billingAddress", request.getBillingAddress());
            previewData.put("zipCode", request.getZipCode());
            previewData.put("executionTime", now.format(DATE_TIME_FORMATTER));

            return ResponseEntity.ok(ApiResponse.success("Preview successful", previewData));
        } catch (PaymentErrorRuntimeException e) {
            return e.response;
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    "INVALID_REQUEST",
                    null,
                    false,
                    null,
                    null);
        } catch (Exception e) {
            log.error("Preview processing error: {}", e.getMessage());
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error: " + e.getMessage(),
                    "SYSTEM_ERROR",
                    "96",
                    true,
                    null,
                    null);
        }
    }

    @PostMapping("/credit-card")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processCreditCardPayment(@RequestBody PaymentRequest request) {
        log.info("Receiving credit card payment request for merchant: {}", request.getMerchantId());

        try {
            ensureIdempotencyKey(request);
            ensurePaymentId(request);
            ensureChannel(request);
            Merchant merchant = resolveMerchant(request.getMerchantId(), request.getPaymentId(), request.getIdempotencyKey());
            String merchantName = merchant.getName();
            String inferredNetwork = firstNonBlank(request.getCardNetwork(), inferCardNetwork(request.getCardNumber()));
            String resolvedCardType = firstNonBlank(request.getCardType(), resolveCardTypeFromNetwork(inferredNetwork));
            Map<String, Object> cmsResponse = cmsClient.authorizePayment(request, merchantName);

            Boolean approved = (Boolean) cmsResponse.get("approved");
            String responseCode = stringValue(cmsResponse.get("responseCode"));
            String responseMessage = stringValue(cmsResponse.get("responseMessage"));
            if (responseMessage == null) {
                responseMessage = stringValue(cmsResponse.get("message"));
            }

            cmsResponse.put("idempotencyKey", request.getIdempotencyKey());
            cmsResponse.putIfAbsent("paymentId", request.getPaymentId());
            cmsResponse.put("merchantId", request.getMerchantId());
            cmsResponse.put("merchantName", merchantName);
            cmsResponse.put("recipientAccount", request.getMerchantId());
            cmsResponse.put("recipientName", merchantName);
            cmsResponse.put("bankName", DEFAULT_BANK_NAME);
            cmsResponse.put("amount", request.getAmount());
            cmsResponse.put("fee", 0);
            cmsResponse.put("totalAmount", request.getAmount());
            cmsResponse.put("currency", resolvePaymentCurrency(cmsResponse));
            cmsResponse.put("cardType", firstNonBlank(stringValue(cmsResponse.get("cardType")), resolvedCardType));
            cmsResponse.put("cardNetwork", firstNonBlank(stringValue(cmsResponse.get("cardNetwork")), inferredNetwork));
            cmsResponse.put("maskedCardNumber", firstNonBlank(stringValue(cmsResponse.get("maskedPan")), maskCardNumber(request.getCardNumber())));

            if (Boolean.TRUE.equals(approved)) {
                LocalDateTime now = LocalDateTime.now();
                cmsResponse.put("transactionTime", now.format(DATE_TIME_FORMATTER));
                cmsResponse.put("transactionId", "TXN" + System.currentTimeMillis() + (int) (Math.random() * 1000));

                return ResponseEntity.ok(ApiResponse.success("Payment successful", cmsResponse));
            }
            return declinedResponse(responseCode,
                    responseMessage != null ? responseMessage : "Unknown error",
                    request.getPaymentId(),
                    request.getIdempotencyKey());

        } catch (PaymentErrorRuntimeException e) {
            return e.response;
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    "INVALID_REQUEST",
                    null,
                    false,
                    request.getPaymentId(),
                    request.getIdempotencyKey());
        } catch (Exception e) {
            log.error("Payment processing error: {}", e.getMessage());
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error: " + e.getMessage(),
                    "SYSTEM_ERROR",
                    "96",
                    true,
                    request.getPaymentId(),
                    request.getIdempotencyKey());
        }
    }

    @PostMapping("/reversal")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reversePayment(@RequestBody PaymentAdjustmentRequest request) {
        return processAdjustment("REVERSAL", request);
    }

    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER', 'SYSTEM')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refundPayment(@RequestBody PaymentAdjustmentRequest request) {
        return processAdjustment("REFUND", request);
    }

    private void ensureIdempotencyKey(PaymentRequest request) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            return;
        }

        String fallbackKey = buildFallbackIdempotencyKey(request);
        request.setIdempotencyKey(fallbackKey);
        log.warn("Generated fallback idempotencyKey for payment request. This is temporary until clients send their own stable key.");
    }

    private void ensurePaymentId(PaymentRequest request) {
        if (request.getPaymentId() != null && !request.getPaymentId().trim().isEmpty()) {
            return;
        }
        String source = request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                ? request.getIdempotencyKey()
                : java.util.UUID.randomUUID().toString();
        request.setPaymentId("PAY-" + source.replaceAll("[^A-Za-z0-9]", "").toUpperCase().substring(0, Math.min(24, source.replaceAll("[^A-Za-z0-9]", "").length())));
        if (request.getPaymentId().length() < 10) {
            request.setPaymentId("PAY-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase());
        }
    }

    private void ensureChannel(PaymentRequest request) {
        if (request.getChannel() == null || request.getChannel().trim().isEmpty()) {
            request.setChannel("ONLINE");
        }
    }

    private String buildFallbackIdempotencyKey(PaymentRequest request) {
        try {
            String raw = String.join("|",
                    safe(request.getCardNumber()),
                    request.getAmount() != null ? request.getAmount().toString() : "",
                    safe(request.getMerchantId()),
                    safe(request.getDateCard()),
                    safe(request.getCvc()));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("AUTO-");
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate idempotency key", e);
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private String inferCardNetwork(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return "UNKNOWN";
        }
        if (cardNumber.startsWith("4")) {
            return "VISA";
        }
        if (cardNumber.startsWith("5")) {
            return "MASTERCARD";
        }
        if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) {
            return "AMEX";
        }
        if (cardNumber.startsWith("35")) {
            return "JCB";
        }
        if (cardNumber.startsWith("6")) {
            return "DISCOVER";
        }
        if (cardNumber.startsWith("9")) {
            return "NAPAS";
        }
        return "UNKNOWN";
    }

    private String resolveCardTypeFromNetwork(String cardNetwork) {
        return "UNKNOWN";
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> processAdjustment(String adjustmentType,
                                                                               PaymentAdjustmentRequest request) {
        try {
            Transaction adjustment = paymentAdjustmentService.applyAdjustment(adjustmentType, request);

            Map<String, Object> response = new HashMap<>();
            response.put("approved", true);
            response.put("adjustmentType", adjustmentType);
            response.put("paymentId", adjustment.getPaymentId());
            response.put("originalPaymentId", adjustment.getOriginalTransactionId());
            response.put("originalTransactionStatus", "REVERSAL".equalsIgnoreCase(adjustmentType) ? "REVERSED" : "REFUNDED");
            response.put("responseCode", adjustment.getResponseCode());
            response.put("responseMessage", adjustment.getResponseMessage());
            response.put("accountNumber", adjustment.getAccountNumber());
            response.put("accountType", adjustment.getAccountType());
            response.put("merchantId", adjustment.getMerchantId());
            response.put("merchantName", adjustment.getMerchantName());
            response.put("amount", adjustment.getAmount());
            response.put("balanceAfter", adjustment.getBalanceAfter());
            response.put("status", adjustment.getStatus());
            response.put("idempotencyKey", adjustment.getIdempotencyKey());
            response.put("channel", adjustment.getChannel());
            response.put("transactionTime", adjustment.getTransactionDate() != null
                    ? adjustment.getTransactionDate().format(DATE_TIME_FORMATTER)
                    : LocalDateTime.now().format(DATE_TIME_FORMATTER));

            return ResponseEntity.ok(ApiResponse.success(adjustmentType + " successful", response));
        } catch (IllegalArgumentException e) {
            log.error("{} failed: {}", adjustmentType, e.getMessage());
            return errorResponse(HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    "INVALID_REQUEST",
                    null,
                    false,
                    request.getPaymentId(),
                    request.getIdempotencyKey());
        } catch (RuntimeException e) {
            log.error("{} failed: {}", adjustmentType, e.getMessage());
            return errorResponse(HttpStatus.NOT_FOUND,
                    e.getMessage(),
                    "TRANSACTION_NOT_FOUND",
                    null,
                    false,
                    request.getPaymentId(),
                    request.getIdempotencyKey());
        } catch (Exception e) {
            log.error("{} processing error: {}", adjustmentType, e.getMessage(), e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error: " + e.getMessage(),
                    "SYSTEM_ERROR",
                    "96",
                    true,
                    request.getPaymentId(),
                    request.getIdempotencyKey());
        }
    }

    private Merchant resolveMerchant(String merchantId, String paymentId, String idempotencyKey) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId or recipientAccount is required");
        }
        try {
            return merchantService.getActiveMerchant(merchantId);
        } catch (RuntimeException ex) {
            throw new PaymentErrorRuntimeException(errorResponse(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage(),
                    "MERCHANT_INVALID",
                    "03",
                    false,
                    paymentId,
                    idempotencyKey
            ));
        }
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> declinedResponse(String responseCode,
                                                                              String responseMessage,
                                                                              String paymentId,
                                                                              String idempotencyKey) {
        String normalizedCode = responseCode != null ? responseCode : "96";
        String errorCode = mapErrorCode(normalizedCode, responseMessage);
        HttpStatus httpStatus = mapHttpStatus(normalizedCode);
        boolean retryable = isRetryable(normalizedCode);
        String message = "Payment failed: " + responseMessage;
        return errorResponse(httpStatus, message, errorCode, normalizedCode, retryable, paymentId, idempotencyKey);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> errorResponse(HttpStatus httpStatus,
                                                                           String message,
                                                                           String errorCode,
                                                                           String responseCode,
                                                                           boolean retryable,
                                                                           String paymentId,
                                                                           String idempotencyKey) {
        PaymentErrorDetail detail = PaymentErrorDetail.builder()
                .errorCode(errorCode)
                .responseCode(responseCode)
                .httpStatus(httpStatus.value())
                .retryable(retryable)
                .paymentId(paymentId)
                .idempotencyKey(idempotencyKey)
                .build();

        Map<String, Object> result = new HashMap<>();
        result.put("errorCode", detail.getErrorCode());
        result.put("responseCode", detail.getResponseCode());
        result.put("httpStatus", detail.getHttpStatus());
        result.put("retryable", detail.isRetryable());
        result.put("paymentId", detail.getPaymentId());
        result.put("idempotencyKey", detail.getIdempotencyKey());

        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .code(httpStatus.value())
                        .message(message)
                        .result(result)
                        .build());
    }

    private String mapErrorCode(String responseCode, String responseMessage) {
        if ("14".equals(responseCode)) {
            return "INVALID_CARD";
        }
        if ("55".equals(responseCode)) {
            return "INVALID_CVV";
        }
        if ("54".equals(responseCode)) {
            if (responseMessage != null && responseMessage.toLowerCase().contains("format")) {
                return "INVALID_EXPIRATION_DATE";
            }
            if (responseMessage != null && responseMessage.toLowerCase().contains("invalid expiration")) {
                return "INVALID_EXPIRATION_DATE";
            }
            return "EXPIRED_CARD";
        }
        if ("51".equals(responseCode)) {
            if (responseMessage != null && responseMessage.toLowerCase().contains("insufficient")) {
                return "INSUFFICIENT_FUNDS";
            }
            return "CREDIT_LIMIT_EXCEEDED";
        }
        if ("94".equals(responseCode)) {
            return "DUPLICATE_REQUEST";
        }
        if ("43".equals(responseCode)) {
            return "CARD_RESTRICTED";
        }
        if ("03".equals(responseCode)) {
            return "MERCHANT_INVALID";
        }
        return "SYSTEM_ERROR";
    }

    private HttpStatus mapHttpStatus(String responseCode) {
        if ("94".equals(responseCode)) {
            return HttpStatus.CONFLICT;
        }
        if ("96".equals(responseCode)) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private boolean isRetryable(String responseCode) {
        return "96".equals(responseCode);
    }

    private String resolvePaymentCurrency(Map<String, Object> cmsResponse) {
        String accountId = stringValue(cmsResponse.get("accountId"));
        String accountType = stringValue(cmsResponse.get("accountType"));

        if (accountId == null || accountType == null) {
            return "USD";
        }

        try {
            if ("SAVINGS".equalsIgnoreCase(accountType)) {
                return savingsAccountService.getAccount(accountId).getCurrency();
            }
            if ("LOAN".equalsIgnoreCase(accountType)) {
                return loanAccountService.getAccount(accountId).getCurrency();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve currency from account {} ({})", accountId, accountType);
        }

        return "USD";
    }

    private static class PaymentErrorRuntimeException extends RuntimeException {
        private final transient ResponseEntity<ApiResponse<Map<String, Object>>> response;

        private PaymentErrorRuntimeException(ResponseEntity<ApiResponse<Map<String, Object>>> response) {
            this.response = response;
        }
    }
}

