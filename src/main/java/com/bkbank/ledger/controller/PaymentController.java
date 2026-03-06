package com.bkbank.ledger.controller;

import com.bkbank.ledger.client.CmsClient;
import com.bkbank.ledger.dto.ApiResponse;
import com.bkbank.ledger.dto.PaymentRequest;
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

    /**
     * POST /payment/credit-card
     * Xử lý thanh toán thẻ tín dụng từ Mobile App (Online Payment)
     */
    @PostMapping("/credit-card")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processCreditCardPayment(@RequestBody PaymentRequest request) {
        log.info("Receiving credit card payment request from mobile for merchant: {}", request.getMerchantId());

        try {
            // In a real system we would look up the merchantId in our database to get the real merchantName
            // For now, using a placeholder based on ID
            String merchantName = "Merchant " + request.getMerchantId();

            if ("SP0001".equals(request.getMerchantId())) merchantName = "Điện lực EVN";
            else if ("SP0002".equals(request.getMerchantId())) merchantName = "Siêu thị GO";
            else if ("SP0003".equals(request.getMerchantId())) merchantName = "Tạp hóa Xanh";

            // Call CMS to authorize payment (which includes CVC mapping and Fineract debit)
            Map<String, Object> cmsResponse = cmsClient.authorizePayment(request, merchantName);

            // CMS returns {"approved": true/false, "responseCode": "...", "responseMessage": "..."}
            Boolean approved = (Boolean) cmsResponse.get("approved");
            String responseCode = (String) cmsResponse.get("responseCode");
            Object responseMessage = cmsResponse.get("responseMessage");

            if (Boolean.TRUE.equals(approved)) {
                return ResponseEntity.ok(ApiResponse.success("Payment successful", cmsResponse));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Payment failed: " + responseMessage));
            }

        } catch (Exception e) {
            log.error("Payment processing error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "Internal Server Error: " + e.getMessage()));
        }
    }
}
