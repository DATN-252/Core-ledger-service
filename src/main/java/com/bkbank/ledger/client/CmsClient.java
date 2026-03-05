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

    /**
     * Fetch cards from CMS by a list of account IDs.
     */
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
}
