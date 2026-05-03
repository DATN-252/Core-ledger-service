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
import java.util.LinkedHashMap;
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
                    "merchantId or recipientAccount is required",
                    false,
                    null,
                    null,
                    Map.of());
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
            previewData.put("merchantAddress", merchant.getDisplayAddress());
            previewData.put("recipientAccount", merchant.getResolvedSettlementAccountNumber());
            previewData.put("recipientName", merchant.getResolvedSettlementAccountName());
            previewData.put("bankName", merchant.getResolvedSettlementBankName());
            previewData.put("latitude", request.getLatitude());
            previewData.put("longitude", request.getLongitude());
            previewData.put("merchantLatitude", merchant.getLatitude());
            previewData.put("merchantLongitude", merchant.getLongitude());
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
                    e.getMessage(),
                    false,
                    null,
                    null,
                    Map.of());
        } catch (Exception e) {
            log.error("Preview processing error: {}", e.getMessage());
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error: " + e.getMessage(),
                    "SYSTEM_ERROR",
                    "96",
                    e.getMessage(),
                    true,
                    null,
                    null,
                    Map.of());
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
            Map<String, Object> cmsResponse = normalizeCmsResponse(cmsClient.authorizePayment(request, merchant));
            LocalDateTime now = LocalDateTime.now();
            String transactionTime = now.format(DATE_TIME_FORMATTER);
            String transactionId = buildTransactionId();

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
            cmsResponse.put("merchantAddress", merchant.getDisplayAddress());
            cmsResponse.put("recipientAccount", merchant.getResolvedSettlementAccountNumber());
            cmsResponse.put("recipientName", merchant.getResolvedSettlementAccountName());
            cmsResponse.put("bankName", merchant.getResolvedSettlementBankName());
            cmsResponse.put("latitude", request.getLatitude());
            cmsResponse.put("longitude", request.getLongitude());
            cmsResponse.put("merchantLatitude", merchant.getLatitude());
            cmsResponse.put("merchantLongitude", merchant.getLongitude());
            cmsResponse.put("amount", request.getAmount());
            cmsResponse.put("fee", 0);
            cmsResponse.put("totalAmount", request.getAmount());
            cmsResponse.put("currency", resolvePaymentCurrency(cmsResponse));
            cmsResponse.put("cardType", firstNonBlank(stringValue(cmsResponse.get("cardType")), resolvedCardType));
            cmsResponse.put("cardNetwork", firstNonBlank(stringValue(cmsResponse.get("cardNetwork")), inferredNetwork));
            cmsResponse.put("maskedCardNumber", firstNonBlank(stringValue(cmsResponse.get("maskedPan")), maskCardNumber(request.getCardNumber())));
            cmsResponse.putIfAbsent("transactionTime", transactionTime);
            cmsResponse.putIfAbsent("transactionId", transactionId);
            cmsResponse.putIfAbsent("paymentNote", request.getPaymentNote());

            if (Boolean.TRUE.equals(approved)) {
                return ResponseEntity.ok(ApiResponse.success("Payment successful", cmsResponse));
            }
            return declinedResponse(responseCode,
                    responseMessage != null ? responseMessage : "Unknown error",
                    request.getPaymentId(),
                    request.getIdempotencyKey(),
                    buildFailureContext(cmsResponse));

        } catch (PaymentErrorRuntimeException e) {
            return e.response;
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    "INVALID_REQUEST",
                    null,
                    e.getMessage(),
                    false,
                    request.getPaymentId(),
                    request.getIdempotencyKey(),
                    buildRequestContext(request, null));
        } catch (Exception e) {
            log.error("Payment processing error: {}", e.getMessage());
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error: " + e.getMessage(),
                    "SYSTEM_ERROR",
                    "96",
                    e.getMessage(),
                    true,
                    request.getPaymentId(),
                    request.getIdempotencyKey(),
                    buildRequestContext(request, null));
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
                    e.getMessage(),
                    false,
                    request.getPaymentId(),
                    request.getIdempotencyKey(),
                    Map.of());
        } catch (RuntimeException e) {
            log.error("{} failed: {}", adjustmentType, e.getMessage());
            return errorResponse(HttpStatus.NOT_FOUND,
                    e.getMessage(),
                    "TRANSACTION_NOT_FOUND",
                    null,
                    e.getMessage(),
                    false,
                    request.getPaymentId(),
                    request.getIdempotencyKey(),
                    Map.of());
        } catch (Exception e) {
            log.error("{} processing error: {}", adjustmentType, e.getMessage(), e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error: " + e.getMessage(),
                    "SYSTEM_ERROR",
                    "96",
                    e.getMessage(),
                    true,
                    request.getPaymentId(),
                    request.getIdempotencyKey(),
                    Map.of());
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
                    ex.getMessage(),
                    false,
                    paymentId,
                    idempotencyKey,
                    Map.of()
            ));
        }
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> declinedResponse(String responseCode,
                                                                              String responseMessage,
                                                                              String paymentId,
                                                                              String idempotencyKey,
                                                                              Map<String, Object> extraResult) {
        String normalizedCode = responseCode != null ? responseCode : "96";
        String errorCode = mapErrorCode(normalizedCode, responseMessage);
        HttpStatus httpStatus = mapHttpStatus(normalizedCode);
        boolean retryable = isRetryable(normalizedCode);
        String message = "Payment failed: " + responseMessage;
        return errorResponse(httpStatus, message, errorCode, normalizedCode, responseMessage, retryable, paymentId, idempotencyKey, extraResult);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> errorResponse(HttpStatus httpStatus,
                                                                           String message,
                                                                           String errorCode,
                                                                           String responseCode,
                                                                           String responseMessage,
                                                                           boolean retryable,
                                                                           String paymentId,
                                                                           String idempotencyKey,
                                                                           Map<String, Object> extraResult) {
        String errorTitle = resolveErrorTitle(errorCode);
        String errorHint = resolveErrorHint(errorCode, retryable);
        PaymentErrorDetail detail = PaymentErrorDetail.builder()
                .errorCode(errorCode)
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .errorTitle(errorTitle)
                .errorHint(errorHint)
                .httpStatus(httpStatus.value())
                .retryable(retryable)
                .paymentId(paymentId)
                .idempotencyKey(idempotencyKey)
                .build();

        Map<String, Object> result = new HashMap<>();
        result.put("errorCode", detail.getErrorCode());
        result.put("responseCode", detail.getResponseCode());
        result.put("responseMessage", detail.getResponseMessage());
        result.put("errorTitle", detail.getErrorTitle());
        result.put("errorHint", detail.getErrorHint());
        result.put("httpStatus", detail.getHttpStatus());
        result.put("retryable", detail.isRetryable());
        result.put("paymentId", detail.getPaymentId());
        result.put("idempotencyKey", detail.getIdempotencyKey());
        if (extraResult != null && !extraResult.isEmpty()) {
            result.putAll(extraResult);
        }

        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .code(httpStatus.value())
                        .message(message)
                        .result(result)
                        .build());
    }

    private String mapErrorCode(String responseCode, String responseMessage) {
        String normalizedMessage = responseMessage != null ? responseMessage.toLowerCase() : "";
        if ("62".equals(responseCode) || normalizedMessage.contains("linked loan account is not active")
                || normalizedMessage.contains("linked savings account is not active")
                || normalizedMessage.contains("loan account is not active")
                || normalizedMessage.contains("account is not active")) {
            return "LINKED_ACCOUNT_INACTIVE";
        }
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
        if ("59".equals(responseCode)) {
            return "SUSPECTED_FRAUD";
        }
        if ("03".equals(responseCode)) {
            return "MERCHANT_INVALID";
        }
        return "SYSTEM_ERROR";
    }

    private String resolveErrorTitle(String errorCode) {
        if (errorCode == null) {
            return "Giao dịch thất bại";
        }
        return switch (errorCode) {
            case "LINKED_ACCOUNT_INACTIVE" -> "Tài khoản liên kết chưa kích hoạt";
            case "INVALID_CARD" -> "Thẻ không hợp lệ";
            case "INVALID_CVV" -> "CVV không đúng";
            case "INVALID_EXPIRATION_DATE" -> "Ngày hết hạn không hợp lệ";
            case "EXPIRED_CARD" -> "Thẻ đã hết hạn";
            case "INSUFFICIENT_FUNDS" -> "Không đủ số dư";
            case "CREDIT_LIMIT_EXCEEDED" -> "Vượt hạn mức tín dụng";
            case "DUPLICATE_REQUEST" -> "Yêu cầu trùng lặp";
            case "CARD_RESTRICTED" -> "Thẻ đang bị hạn chế";
            case "SUSPECTED_FRAUD" -> "Nghi ngờ gian lận";
            case "MERCHANT_INVALID" -> "Merchant không hợp lệ";
            default -> "Lỗi hệ thống";
        };
    }

    private String resolveErrorHint(String errorCode, boolean retryable) {
        if (errorCode == null) {
            return retryable ? "Vui lòng thử lại sau ít phút." : "Vui lòng kiểm tra lại thông tin giao dịch.";
        }
        return switch (errorCode) {
            case "LINKED_ACCOUNT_INACTIVE" -> "Kích hoạt tài khoản liên kết trước khi thanh toán.";
            case "INVALID_CARD" -> "Kiểm tra lại số thẻ và thử lại.";
            case "INVALID_CVV" -> "Kiểm tra lại mã CVV ở mặt sau thẻ.";
            case "INVALID_EXPIRATION_DATE" -> "Kiểm tra lại định dạng ngày hết hạn MM/YY.";
            case "EXPIRED_CARD" -> "Sử dụng thẻ khác hoặc gia hạn thẻ.";
            case "INSUFFICIENT_FUNDS" -> "Nạp thêm tiền hoặc chọn tài khoản khác.";
            case "CREDIT_LIMIT_EXCEEDED" -> "Giảm số tiền hoặc thanh toán dư nợ trước.";
            case "DUPLICATE_REQUEST" -> "Yêu cầu này đã được xử lý trước đó.";
            case "CARD_RESTRICTED" -> "Liên hệ ngân hàng hoặc mở khóa thẻ trước khi thử lại.";
            case "SUSPECTED_FRAUD" -> "Xác minh giao dịch hoặc liên hệ ngân hàng nếu đây là giao dịch hợp lệ.";
            case "MERCHANT_INVALID" -> "Kiểm tra lại merchant hoặc chọn đơn vị chấp nhận khác.";
            default -> retryable ? "Vui lòng thử lại sau ít phút." : "Vui lòng liên hệ hỗ trợ nếu lỗi tiếp tục xảy ra.";
        };
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
        String accountId = stringValue(cmsResponse.get("linkedAccountNumber"));
        if (accountId == null) {
            accountId = stringValue(cmsResponse.get("accountId"));
        }
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

    private Map<String, Object> normalizeCmsResponse(Map<String, Object> cmsResponse) {
        if (cmsResponse == null) {
            return Map.of(
                    "approved", false,
                    "responseCode", "96",
                    "responseMessage", "System error: Empty CMS response"
            );
        }

        Object nestedResult = cmsResponse.get("result");
        if (nestedResult instanceof Map<?, ?> nestedMap) {
            Map<String, Object> normalized = new HashMap<>();
            nestedMap.forEach((key, value) -> normalized.put(String.valueOf(key), value));

            if (!normalized.containsKey("approved")) {
                normalized.put("approved", false);
            }

            if (!normalized.containsKey("responseCode")) {
                Object topLevelCode = cmsResponse.get("responseCode");
                if (topLevelCode != null) {
                    normalized.put("responseCode", topLevelCode);
                }
            }

            if (!normalized.containsKey("responseMessage")) {
                Object topLevelMessage = cmsResponse.get("responseMessage");
                if (topLevelMessage == null) {
                    topLevelMessage = cmsResponse.get("message");
                }
                if (topLevelMessage != null) {
                    normalized.put("responseMessage", topLevelMessage);
                }
            }

            return normalized;
        }

        return cmsResponse;
    }

    private Map<String, Object> buildFailureContext(Map<String, Object> cmsResponse) {
        Map<String, Object> context = new LinkedHashMap<>();
        putIfPresent(context, "transactionId", cmsResponse.get("transactionId"));
        putIfPresent(context, "transactionTime", cmsResponse.get("transactionTime"));
        putIfPresent(context, "merchantId", cmsResponse.get("merchantId"));
        putIfPresent(context, "merchantName", cmsResponse.get("merchantName"));
        putIfPresent(context, "merchantAddress", cmsResponse.get("merchantAddress"));
        putIfPresent(context, "recipientAccount", cmsResponse.get("recipientAccount"));
        putIfPresent(context, "recipientName", cmsResponse.get("recipientName"));
        putIfPresent(context, "bankName", cmsResponse.get("bankName"));
        putIfPresent(context, "amount", cmsResponse.get("amount"));
        putIfPresent(context, "fee", cmsResponse.get("fee"));
        putIfPresent(context, "totalAmount", cmsResponse.get("totalAmount"));
        putIfPresent(context, "currency", cmsResponse.get("currency"));
        putIfPresent(context, "cardType", cmsResponse.get("cardType"));
        putIfPresent(context, "cardNetwork", cmsResponse.get("cardNetwork"));
        putIfPresent(context, "maskedCardNumber", cmsResponse.get("maskedCardNumber"));
        putIfPresent(context, "latitude", cmsResponse.get("latitude"));
        putIfPresent(context, "longitude", cmsResponse.get("longitude"));
        putIfPresent(context, "merchantLatitude", cmsResponse.get("merchantLatitude"));
        putIfPresent(context, "merchantLongitude", cmsResponse.get("merchantLongitude"));
        putIfPresent(context, "paymentNote", cmsResponse.get("paymentNote"));
        return context;
    }

    private Map<String, Object> buildRequestContext(PaymentRequest request, Merchant merchant) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("transactionId", buildTransactionId());
        context.put("transactionTime", LocalDateTime.now().format(DATE_TIME_FORMATTER));
        putIfPresent(context, "merchantId", request.getMerchantId());
        putIfPresent(context, "merchantName", merchant != null ? merchant.getName() : null);
        putIfPresent(context, "merchantAddress", merchant != null ? merchant.getDisplayAddress() : null);
        putIfPresent(context, "recipientAccount", merchant != null ? merchant.getResolvedSettlementAccountNumber() : null);
        putIfPresent(context, "recipientName", merchant != null ? merchant.getResolvedSettlementAccountName() : null);
        putIfPresent(context, "bankName", merchant != null ? merchant.getResolvedSettlementBankName() : null);
        putIfPresent(context, "amount", request.getAmount());
        context.put("fee", 0);
        putIfPresent(context, "totalAmount", request.getAmount());
        putIfPresent(context, "currency", firstNonBlank(request.getCurrency(), "VND"));
        putIfPresent(context, "cardType", request.getCardType());
        putIfPresent(context, "cardNetwork", firstNonBlank(request.getCardNetwork(), inferCardNetwork(request.getCardNumber())));
        putIfPresent(context, "maskedCardNumber", maskCardNumber(request.getCardNumber()));
        putIfPresent(context, "latitude", request.getLatitude());
        putIfPresent(context, "longitude", request.getLongitude());
        putIfPresent(context, "merchantLatitude", merchant != null ? merchant.getLatitude() : null);
        putIfPresent(context, "merchantLongitude", merchant != null ? merchant.getLongitude() : null);
        putIfPresent(context, "paymentNote", request.getPaymentNote());
        return context;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private String buildTransactionId() {
        return "TXN" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    private static class PaymentErrorRuntimeException extends RuntimeException {
        private final transient ResponseEntity<ApiResponse<Map<String, Object>>> response;

        private PaymentErrorRuntimeException(ResponseEntity<ApiResponse<Map<String, Object>>> response) {
            this.response = response;
        }
    }
}

