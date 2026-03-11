package com.bkbank.ledger.controller;

import com.bkbank.ledger.client.CmsClient;
import com.bkbank.ledger.dto.ApiResponse;
import com.bkbank.ledger.dto.PaymentRequest;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final CmsClient cmsClient;
    private final MerchantService merchantService;

    /**
     * POST /payment/preview
     * Lấy thông tin merchant và hóa đơn trước khi thanh toán
     */
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
            if (cardNumber != null && !cardNumber.trim().isEmpty()) {
                previewData.put("cardNetwork", getCardNetwork(cardNumber));
            }

            return ResponseEntity.ok(ApiResponse.success("Preview successful", previewData));
        } catch (Exception e) {
            log.error("Preview processing error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "Internal Server Error: " + e.getMessage()));
        }
    }

    /**
     * POST /payment/credit-card
     * Xử lý thanh toán thẻ tín dụng từ Mobile App (Online Payment)
     */
    @PostMapping("/credit-card")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processCreditCardPayment(@RequestBody PaymentRequest request) {
        log.info("Receiving credit card payment request from mobile for merchant: {}", request.getMerchantId());

        try {
            // Find and validate the merchant
            Merchant merchant = merchantService.getActiveMerchant(request.getMerchantId());
            String merchantName = merchant.getName();

            // Call CMS to authorize payment (which includes CVC mapping and Fineract debit)
            Map<String, Object> cmsResponse = cmsClient.authorizePayment(request, merchantName);

            // CMS returns {"approved": true/false, "responseCode": "...", "responseMessage": "..."}
            Boolean approved = (Boolean) cmsResponse.get("approved");
            String responseCode = (String) cmsResponse.get("responseCode");
            Object responseMessage = cmsResponse.get("responseMessage");

            if (Boolean.TRUE.equals(approved)) {
                // Ensure cardNetwork is included in the response for UI
                cmsResponse.put("cardNetwork", getCardNetwork(request.getCardNumber()));
                
                // Add Transaction Metadata for the Receipt Screen
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                cmsResponse.put("transactionTime", now.format(formatter));
                cmsResponse.put("transactionId", "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000));

                return ResponseEntity.ok(ApiResponse.success("Payment successful", cmsResponse));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Payment failed: " + responseMessage));
            }

        } catch (Exception e) {
            log.error("Payment processing error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "Internal Server Error: " + e.getMessage()));
        }
    }

    private String getCardNetwork(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "UNKNOWN";
        }
        if (cardNumber.startsWith("4")) {
            return "VISA";
        } else if (cardNumber.startsWith("5")) {
            return "MASTERCARD";
        } else if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) {
            return "AMEX";
        } else if (cardNumber.startsWith("35")) {
            return "JCB";
        } else if (cardNumber.startsWith("6")) {
            return "DISCOVER";
        } else if (cardNumber.startsWith("9")) {
            return "NAPAS"; 
        }
        return "UNKNOWN";
    }
}
