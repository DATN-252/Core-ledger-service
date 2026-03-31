package com.bkbank.ledger.controller;

import com.bkbank.ledger.dto.request.FraudAlertNotificationRequest;
import com.bkbank.ledger.dto.response.ApiResponse;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.SavingsAccountRepository;
import com.bkbank.ledger.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/fraud-alerts")
@RequiredArgsConstructor
@Slf4j
public class InternalFraudAlertController {

    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final PushNotificationService pushNotificationService;

    @PostMapping("/notify")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> notifyFraudAlert(@RequestBody FraudAlertNotificationRequest request) {
        try {
            String clientId = resolveClientId(request.getAccountId(), request.getAccountType());
            if (clientId == null || clientId.isBlank()) {
                throw new RuntimeException("Khong tim thay clientId cho account");
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("type", "FRAUD_ALERT");
            data.put("fraudAlertId", request.getFraudAlertId());
            data.put("paymentId", request.getPaymentId());
            data.put("maskedPan", request.getMaskedPan());
            data.put("accountId", request.getAccountId());
            data.put("accountType", request.getAccountType());
            data.put("merchantName", request.getMerchantName());
            data.put("amount", request.getAmount());
            data.put("currency", request.getCurrency());
            data.put("riskLevel", request.getRiskLevel());

            String amountText = String.format("%.2f %s", request.getAmount() != null ? request.getAmount() : 0.0, request.getCurrency() != null ? request.getCurrency() : "USD");
            String body = String.format("Phat hien giao dich nghi ngo %s tai %s. Day co phai ban khong?",
                    amountText,
                    request.getMerchantName() != null ? request.getMerchantName() : "merchant khong xac dinh");

            int sentCount = pushNotificationService.sendToClientTokens(clientId, "Canh bao gian lan", body, data);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("clientId", clientId);
            result.put("sentCount", sentCount);
            return ResponseEntity.ok(ApiResponse.success("Gui fraud alert notification thanh cong", result));
        } catch (Exception e) {
            log.error("Error sending fraud alert notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    private String resolveClientId(String accountId, String accountType) {
        if (accountId == null || accountType == null) {
            return null;
        }
        if ("SAVINGS".equalsIgnoreCase(accountType)) {
            return savingsAccountRepository.findClientIdByAccountNumber(accountId).orElse(null);
        }
        if ("LOAN".equalsIgnoreCase(accountType)) {
            return loanAccountRepository.findClientIdByAccountNumber(accountId).orElse(null);
        }
        return null;
    }
}
