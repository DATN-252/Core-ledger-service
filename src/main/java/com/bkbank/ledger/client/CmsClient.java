package com.bkbank.ledger.client;

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
            String url = UriComponentsBuilder.fromHttpUrl(cmsUrl + "/api/cards")
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

        } catch (Exception e) {
            log.error("Failed to fetch cards from CMS for accounts {}: {}", accountIds, e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> authorizePayment(com.bkbank.ledger.dto.PaymentRequest request, String merchantName) {
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
            payload.put("merchantName", merchantName);
            payload.put("cvc", request.getCvc());
            payload.put("expirationDate", request.getDateCard());
            payload.put("idempotencyKey", request.getIdempotencyKey());

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
}
