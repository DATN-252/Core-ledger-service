package com.bkbank.ledger.controller;

import com.bkbank.ledger.client.CmsClient;
import com.bkbank.ledger.dto.ApiResponse;
import com.bkbank.ledger.dto.PaymentRequest;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.service.MerchantService;
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
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final CmsClient cmsClient;
    private final MerchantService merchantService;

    @PostMapping("/preview")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewPayment(@RequestBody Map<String, Object> request) {
        String merchantId = (String) request.get("merchantId");
        if (merchantId == null || merchantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "merchantId is required"));
        }

        try {
            String cardNumber = (String) request.get("cardNumber");

            Merchant merchant = merchantService.getActiveMerchant(merchantId);
            String merchantName = merchant.getName();

            Double amount = null;
            Object reqAmount = request.get("amount");
            if (reqAmount != null) {
                amount = Double.valueOf(reqAmount.toString());
            }

            Map<String, Object> previewData = new java.util.HashMap<>();
            previewData.put("merchantId", merchantId);
            previewData.put("merchantName", merchantName);
            previewData.put("amount", amount);
            previewData.put("fee", 0);
            previewData.put("status", "VALID");

            Object reqNetwork = request.get("cardNetwork");
            if (reqNetwork != null) {
                previewData.put("cardNetwork", reqNetwork.toString());
            } else if (cardNumber != null && !cardNumber.trim().isEmpty()) {
                previewData.put("cardNetwork", "UNKNOWN");
            }

            return ResponseEntity.ok(ApiResponse.success("Preview successful", previewData));
        } catch (Exception e) {
            log.error("Preview processing error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "Internal Server Error: " + e.getMessage()));
        }
    }

    @PostMapping("/credit-card")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processCreditCardPayment(@RequestBody PaymentRequest request) {
        log.info("Receiving credit card payment request for merchant: {}", request.getMerchantId());

        try {
            Merchant merchant = merchantService.getActiveMerchant(request.getMerchantId());
            String merchantName = merchant.getName();

            ensureIdempotencyKey(request);
            Map<String, Object> cmsResponse = cmsClient.authorizePayment(request, merchantName);

            Boolean approved = (Boolean) cmsResponse.get("approved");
            String responseCode = stringValue(cmsResponse.get("responseCode"));
            String responseMessage = stringValue(cmsResponse.get("responseMessage"));
            if (responseMessage == null) {
                responseMessage = stringValue(cmsResponse.get("message"));
            }

            cmsResponse.put("idempotencyKey", request.getIdempotencyKey());

            if (Boolean.TRUE.equals(approved)) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                cmsResponse.put("transactionTime", now.format(formatter));
                cmsResponse.put("transactionId", "TXN" + System.currentTimeMillis() + (int) (Math.random() * 1000));

                return ResponseEntity.ok(ApiResponse.success("Payment successful", cmsResponse));
            }

            if ("94".equals(responseCode)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(409, responseMessage != null ? responseMessage : "Duplicate payment request"));
            }

            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Payment failed: " + (responseMessage != null ? responseMessage : "Unknown error")));

        } catch (Exception e) {
            log.error("Payment processing error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "Internal Server Error: " + e.getMessage()));
        }
    }

    private void ensureIdempotencyKey(PaymentRequest request) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            return;
        }

        String fallbackKey = buildFallbackIdempotencyKey(request);
        request.setIdempotencyKey(fallbackKey);
        log.warn("Generated fallback idempotencyKey for payment request. This is temporary until clients send their own stable key.");
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
}
