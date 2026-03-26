package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.PushDeviceToken;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.event.TransactionNotificationEvent;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.SavingsAccountRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final TransactionRepository transactionRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final PushDeviceTokenService pushDeviceTokenService;
    private final ObjectMapper objectMapper;

    @Value("${push.expo.enabled:true}")
    private boolean pushEnabled;

    @Value("${push.expo.url:https://exp.host/--/api/v2/push/send}")
    private String expoPushUrl;

    @Value("${push.expo.access-token:}")
    private String expoAccessToken;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCommitted(TransactionNotificationEvent event) {
        transactionRepository.findById(event.transactionId()).ifPresent(this::sendTransactionNotificationSafely);
    }

    public void sendTransactionNotificationSafely(Transaction transaction) {
        try {
            sendTransactionNotification(transaction);
        } catch (Exception e) {
            log.error("Failed to send push notification for transaction {}: {}", transaction.getId(), e.getMessage(), e);
        }
    }

    public void sendTransactionNotification(Transaction transaction) throws Exception {
        if (!pushEnabled) {
            return;
        }

        Client client = resolveClient(transaction);
        if (client == null || client.getClientId() == null) {
            return;
        }

        List<PushDeviceToken> deviceTokens = pushDeviceTokenService.getActiveTokens(client.getClientId());
        if (deviceTokens.isEmpty()) {
            return;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (PushDeviceToken deviceToken : deviceTokens) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("to", deviceToken.getExpoPushToken());
            payload.put("title", buildTitle(transaction));
            payload.put("body", buildBody(transaction));
            payload.put("sound", "default");
            payload.put("data", buildData(transaction));
            messages.add(payload);
        }

        HttpResponse<String> response = sendExpoMessages(messages);
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Expo push send failed: " + response.body());
        }

        handleExpoResponse(deviceTokens, response.body());
    }

    public void sendToSingleToken(String expoPushToken,
                                  String title,
                                  String body,
                                  Map<String, Object> data) throws Exception {
        if (!pushEnabled) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("to", expoPushToken);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("sound", "default");
        payload.put("data", data != null ? data : new HashMap<>());

        HttpResponse<String> response = sendExpoMessages(List.of(payload));
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Expo push send failed: " + response.body());
        }

        handleExpoResponse(List.of(createPseudoDeviceToken(expoPushToken)), response.body());
    }

    private Client resolveClient(Transaction transaction) {
        if ("SAVINGS".equalsIgnoreCase(transaction.getAccountType())) {
            return savingsAccountRepository.findByAccountNumber(transaction.getAccountNumber())
                    .map(account -> account.getClient())
                    .orElse(null);
        }
        if ("LOAN".equalsIgnoreCase(transaction.getAccountType())) {
            return loanAccountRepository.findByAccountNumber(transaction.getAccountNumber())
                    .map(account -> account.getClient())
                    .orElse(null);
        }
        return null;
    }

    private String buildTitle(Transaction transaction) {
        if ("FAILED".equalsIgnoreCase(transaction.getStatus())) {
            return "Giao dich that bai";
        }
        return "Giao dich thanh cong";
    }

    private String buildBody(Transaction transaction) {
        String amount = formatAmount(transaction.getAmount(), transaction.getCurrency());
        String merchantName = transaction.getMerchantName() != null && !transaction.getMerchantName().isBlank()
                ? transaction.getMerchantName()
                : transaction.getAccountNumber();
        if ("FAILED".equalsIgnoreCase(transaction.getStatus())) {
            return "Giao dich " + amount + " tai " + merchantName + " khong thanh cong";
        }
        if ("PAYMENT".equalsIgnoreCase(transaction.getTransactionType())) {
            return "Ban vua thanh toan " + amount + " vao tai khoan " + transaction.getAccountNumber();
        }
        if ("REFUND".equalsIgnoreCase(transaction.getTransactionType()) || "REVERSAL".equalsIgnoreCase(transaction.getTransactionType())) {
            return "Tai khoan cua ban vua duoc hoan " + amount + " tu " + merchantName;
        }
        return "Ban vua thuc hien giao dich " + amount + " tai " + merchantName;
    }

    private Map<String, Object> buildData(Transaction transaction) {
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", transaction.getId());
        data.put("paymentId", transaction.getPaymentId());
        data.put("accountNumber", transaction.getAccountNumber());
        data.put("transactionType", transaction.getTransactionType());
        data.put("status", transaction.getStatus());
        data.put("amount", transaction.getAmount());
        data.put("currency", transaction.getCurrency());
        data.put("merchantId", transaction.getMerchantId());
        data.put("merchantName", transaction.getMerchantName());
        return data;
    }

    private void handleExpoResponse(List<PushDeviceToken> deviceTokens, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.isArray()) {
                return;
            }
            for (int i = 0; i < Math.min(deviceTokens.size(), dataNode.size()); i++) {
                JsonNode item = dataNode.get(i);
                if (!"error".equalsIgnoreCase(item.path("status").asText())) {
                    continue;
                }
                String errorCode = item.path("details").path("error").asText("");
                if ("DeviceNotRegistered".equalsIgnoreCase(errorCode)) {
                    pushDeviceTokenService.markTokenInvalid(deviceTokens.get(i).getExpoPushToken());
                }
            }
        } catch (Exception e) {
            log.warn("Unable to parse Expo push response: {}", e.getMessage());
        }
    }

    private String formatAmount(Double amount, String currency) {
        return String.format("%.2f %s", amount != null ? amount : 0.0, currency != null ? currency : "USD");
    }

    private HttpResponse<String> sendExpoMessages(List<Map<String, Object>> messages) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(expoPushUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(messages)));

        if (expoAccessToken != null && !expoAccessToken.isBlank()) {
            builder.header("Authorization", "Bearer " + expoAccessToken.trim());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private PushDeviceToken createPseudoDeviceToken(String expoPushToken) {
        PushDeviceToken token = new PushDeviceToken();
        token.setExpoPushToken(expoPushToken);
        return token;
    }
}
