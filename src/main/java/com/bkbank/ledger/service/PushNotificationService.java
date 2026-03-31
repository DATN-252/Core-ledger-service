package com.bkbank.ledger.service;

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
    private static final String DEFAULT_ANDROID_CHANNEL_ID = "default";

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

        if (isFraudDecline(transaction)) {
            log.info("Skip balance-change push for fraud-declined transaction {}", transaction.getId());
            return;
        }

        String clientId = resolveClientId(transaction);
        if (clientId == null || clientId.isBlank()) {
            return;
        }

        List<PushDeviceToken> deviceTokens = pushDeviceTokenService.getActiveTokens(clientId);
        if (deviceTokens.isEmpty()) {
            return;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (PushDeviceToken deviceToken : deviceTokens) {
            messages.add(buildExpoPayload(
                    deviceToken.getExpoPushToken(),
                    buildTitle(transaction),
                    buildBody(transaction),
                    buildData(transaction)
            ));
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

        Map<String, Object> payload = buildExpoPayload(expoPushToken, title, body, data);

        HttpResponse<String> response = sendExpoMessages(List.of(payload));
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Expo push send failed: " + response.body());
        }

        handleExpoResponse(List.of(createPseudoDeviceToken(expoPushToken)), response.body());
    }

    public int sendToClientTokens(String clientId,
                                  String title,
                                  String body,
                                  Map<String, Object> data) throws Exception {
        if (!pushEnabled) {
            return 0;
        }

        List<PushDeviceToken> deviceTokens = pushDeviceTokenService.getActiveTokens(clientId);
        if (deviceTokens.isEmpty()) {
            throw new IllegalArgumentException("Khong tim thay push token active cho khach hang nay");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (PushDeviceToken deviceToken : deviceTokens) {
            messages.add(buildExpoPayload(deviceToken.getExpoPushToken(), title, body, data));
        }

        HttpResponse<String> response = sendExpoMessages(messages);
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Expo push send failed: " + response.body());
        }

        handleExpoResponse(deviceTokens, response.body());
        return deviceTokens.size();
    }

    private String resolveClientId(Transaction transaction) {
        if ("SAVINGS".equalsIgnoreCase(transaction.getAccountType())) {
            return savingsAccountRepository.findClientIdByAccountNumber(transaction.getAccountNumber())
                    .orElse(null);
        }
        if ("LOAN".equalsIgnoreCase(transaction.getAccountType())) {
            return loanAccountRepository.findClientIdByAccountNumber(transaction.getAccountNumber())
                    .orElse(null);
        }
        return null;
    }

    private String buildTitle(Transaction transaction) {
        return "Bien dong so du";
    }

    private String buildBody(Transaction transaction) {
        String amount = formatAmount(transaction.getAmount(), transaction.getCurrency());
        String accountRef = maskAccountNumber(transaction.getAccountNumber());
        String balanceText = buildBalanceText(transaction);

        if ("FAILED".equalsIgnoreCase(transaction.getStatus())) {
            return String.format("TK %s giao dich %s khong thanh cong.%s",
                    accountRef,
                    amount,
                    balanceText);
        }

        if (isCreditTransaction(transaction)) {
            return String.format("TK %s vua ghi co %s.%s",
                    accountRef,
                    amount,
                    balanceText);
        }

        return String.format("TK %s vua ghi no %s.%s",
                accountRef,
                amount,
                balanceText);
    }

    private Map<String, Object> buildData(Transaction transaction) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", transaction.getId() != null ? transaction.getId().toString() : null);
        result.put("accountNumber", transaction.getAccountNumber());
        result.put("accountType", transaction.getAccountType());
        result.put("transactionType", transaction.getTransactionType());
        result.put("amount", transaction.getAmount());
        result.put("currency", transaction.getCurrency());
        result.put("paymentId", transaction.getPaymentId());
        result.put("idempotencyKey", transaction.getIdempotencyKey());
        result.put("originalTransactionId", transaction.getOriginalTransactionId());
        result.put("channel", transaction.getChannel());
        result.put("transactionDate", transaction.getTransactionDate() != null ? transaction.getTransactionDate().toString() : null);
        result.put("description", transaction.getDescription());
        result.put("balanceAfter", transaction.getBalanceAfter());
        result.put("merchantId", transaction.getMerchantId());
        result.put("merchantName", transaction.getMerchantName());
        result.put("location", transaction.getLocation());
        result.put("latitude", transaction.getLatitude());
        result.put("longitude", transaction.getLongitude());
        result.put("cardNetwork", transaction.getCardNetwork());
        result.put("authCode", transaction.getAuthCode());
        result.put("stan", transaction.getStan());
        result.put("rrn", transaction.getRrn());
        result.put("externalReference", transaction.getExternalReference());
        result.put("responseCode", transaction.getResponseCode());
        result.put("responseMessage", transaction.getResponseMessage());
        result.put("status", transaction.getStatus());

        Map<String, Object> data = new HashMap<>();
        data.put("result", result);
        return data;
    }

    private Map<String, Object> buildExpoPayload(String expoPushToken,
                                                 String title,
                                                 String body,
                                                 Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", expoPushToken);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("sound", "default");
        payload.put("channelId", DEFAULT_ANDROID_CHANNEL_ID);
        payload.put("data", data != null ? data : new HashMap<>());
        return payload;
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

    private boolean isFraudDecline(Transaction transaction) {
        if (transaction == null || !"FAILED".equalsIgnoreCase(transaction.getStatus())) {
            return false;
        }
        if ("59".equalsIgnoreCase(transaction.getResponseCode())) {
            return true;
        }
        return containsIgnoreCase(transaction.getResponseMessage(), "suspected fraud")
                || containsIgnoreCase(transaction.getDescription(), "suspected fraud");
    }

    private boolean containsIgnoreCase(String value, String expected) {
        return value != null && value.toLowerCase().contains(expected.toLowerCase());
    }

    private boolean isCreditTransaction(Transaction transaction) {
        String transactionType = transaction.getTransactionType();
        return "DEPOSIT".equalsIgnoreCase(transactionType)
                || "REFUND".equalsIgnoreCase(transactionType)
                || "REVERSAL".equalsIgnoreCase(transactionType)
                || "SETTLEMENT".equalsIgnoreCase(transactionType);
    }

    private String buildBalanceText(Transaction transaction) {
        if (transaction.getBalanceAfter() == null) {
            return "";
        }

        String label = "LOAN".equalsIgnoreCase(transaction.getAccountType())
                ? " Du no sau GD: "
                : " So du sau GD: ";
        return label + formatAmount(transaction.getBalanceAfter(), transaction.getCurrency());
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "****";
        }
        if (accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
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
