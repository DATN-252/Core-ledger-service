package com.bkbank.ledger.client;

import com.bkbank.ledger.entity.Merchant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class CmsClient {
    private static final Logger log = LoggerFactory.getLogger(CmsClient.class);

    private final RestTemplate restTemplate;

    @Value("${cms.url}")
    private String cmsUrl;

    @Value("${cms.internal-api-key}")
    private String cmsApiKey;

    public CmsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> getCardsByAccountIds(List<String> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(cmsUrl + "/api/internal/cards")
                    .queryParam("accountIds", String.join(",", accountIds))
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", cmsApiKey);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            log.info("Fetched {} cards from CMS for accounts: {}",
                    response.getBody() != null ? response.getBody().size() : 0, accountIds);

            return response.getBody() != null ? response.getBody() : List.of();

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Failed to fetch cards from CMS for accounts {}. Status: {}, Response: {}",
                    accountIds, e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch cards from CMS for accounts {}: {}", accountIds, e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getCard(Long cardId) {
        try {
            String url = cmsUrl + "/api/internal/cards/" + cardId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", cmsApiKey);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch card {} from CMS: {}", cardId, e.getMessage());
            return null;
        }
    }

    /**
     * Get card details by card number (for payment validation)
     * @param cardNumber the card number
     * @return card details map or null if not found
     */
    public Map<String, Object> getCardDetails(String cardNumber) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(cmsUrl + "/api/internal/cards/by-number")
                    .queryParam("cardNumber", cardNumber)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", cmsApiKey);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.debug("Card not found in CMS: {}", cardNumber);
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch card details from CMS for card number: {}", cardNumber);
            return null;
        }
    }

    public Map<String, Object> blockCard(Long cardId) {
        return postCardLifecycleAction(cardId, "block");
    }

    public Map<String, Object> unblockCard(Long cardId) {
        return postCardLifecycleAction(cardId, "unblock");
    }

    public Map<String, Object> cancelCard(Long cardId) {
        return postCardLifecycleAction(cardId, "cancel");
    }

    public List<Map<String, Object>> getFraudAlertsByAccountIds(List<String> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(cmsUrl + "/api/internal/fraud-alerts")
                    .queryParam("accountIds", String.join(",", accountIds))
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", cmsApiKey);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch fraud alerts from CMS: {}", e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getFraudAlertDetail(Long alertId) {
        try {
            String url = cmsUrl + "/api/internal/fraud-alerts/" + alertId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", cmsApiKey);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch fraud alert {} from CMS: {}", alertId, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> confirmFraudAlert(Long alertId, String note) {
        return postFraudAlertAction(alertId, "confirm", note);
    }

    public Map<String, Object> rejectFraudAlert(Long alertId, String note) {
        return postFraudAlertAction(alertId, "reject", note);
    }

    public Map<String, Object> authorizePayment(com.bkbank.ledger.dto.request.PaymentRequest request, Merchant merchant) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(cmsUrl + "/api/transaction")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", cmsApiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("cardNumber", request.getCardNumber());
            payload.put("amount", request.getAmount());
            payload.put("merchantId", request.getMerchantId());
            payload.put("merchantName", merchant.getName());
            payload.put("merchantAddress", merchant.getDisplayAddress());
            payload.put("merchantCategory", merchant.getCategory());
            payload.put("merchantLatitude", merchant.getLatitude());
            payload.put("merchantLongitude", merchant.getLongitude());
            payload.put("merchantCityPopulation",
                    merchant.getCityReference() != null ? merchant.getCityReference().getPopulation() : null);
            payload.put("cvc", request.getCvc());
            payload.put("expirationDate", request.getDateCard());
            payload.put("cardType", request.getCardType());
            payload.put("cardNetwork", request.getCardNetwork());
            payload.put("cardholderName", request.getCardholderName());
            payload.put("billingAddress", request.getBillingAddress());
            payload.put("zipCode", request.getZipCode());
            payload.put("location", request.getLocation());
            payload.put("latitude", request.getLatitude());
            payload.put("longitude", request.getLongitude());
            payload.put("paymentId", request.getPaymentId());
            payload.put("idempotencyKey", request.getIdempotencyKey());
            payload.put("originalTransactionId", request.getOriginalTransactionId());
            payload.put("channel", request.getChannel());
            payload.put("paymentNote", request.getPaymentNote());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            log.info("Payment authorization response from CMS: {}", response.getBody());
            return response.getBody();

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Failed to authorize payment via CMS. Status: {}, Response: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(e.getResponseBodyAsString(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
            } catch (Exception ex) {
                return Map.of("approved", false, "responseCode", "96", "responseMessage", "System error: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to authorize payment via CMS: {}", e.getMessage());
            return Map.of("approved", false, "responseCode", "96", "responseMessage", "System error: " + e.getMessage());
        }
    }

    private Map<String, Object> postFraudAlertAction(Long alertId, String action, String note) {
        try {
            String url = cmsUrl + "/api/internal/fraud-alerts/" + alertId + "/" + action;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", cmsApiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("note", note);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to {} fraud alert {} in CMS: {}", action, alertId, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> postCardLifecycleAction(Long cardId, String action) {
        try {
            String url = cmsUrl + "/api/internal/cards/" + cardId + "/" + action;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", cmsApiKey);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to {} card {} in CMS: {}", action, cardId, e.getMessage());
            return null;
        }
    }
}
