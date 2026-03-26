package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.request.PushTokenRegistrationRequest;
import com.bkbank.ledger.dto.request.PushTokenUnregisterRequest;
import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.PushDeviceToken;
import com.bkbank.ledger.entity.PushDeviceToken.TokenStatus;
import com.bkbank.ledger.repository.PushDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushDeviceTokenService {

    private final PushDeviceTokenRepository pushDeviceTokenRepository;

    @Transactional
    public PushDeviceToken registerToken(Client client, PushTokenRegistrationRequest request) {
        String expoPushToken = normalizeToken(request.getExpoPushToken());
        if (expoPushToken == null || expoPushToken.isBlank()) {
            throw new IllegalArgumentException("Missing expoPushToken");
        }

        PushDeviceToken token = resolveExistingToken(client.getClientId(), request.getDeviceId(), expoPushToken);
        if (token == null) {
            token = new PushDeviceToken();
        }

        token.setClient(client);
        token.setExpoPushToken(expoPushToken);
        token.setDeviceId(blankToNull(request.getDeviceId()));
        token.setPlatform(blankToNull(request.getPlatform()));
        token.setAppVersion(blankToNull(request.getAppVersion()));
        token.setStatus(TokenStatus.ACTIVE);
        token.setLastSeenAt(LocalDateTime.now());
        PushDeviceToken saved = pushDeviceTokenRepository.save(token);
        log.info("Registered expo push token for client {} device {}", client.getClientId(), saved.getDeviceId());
        return saved;
    }

    @Transactional
    public void unregisterToken(Client client, PushTokenUnregisterRequest request) {
        PushDeviceToken token = null;
        if (request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
            token = pushDeviceTokenRepository.findByClient_ClientIdAndDeviceId(client.getClientId(), request.getDeviceId()).orElse(null);
        }
        if (token == null && request.getExpoPushToken() != null && !request.getExpoPushToken().isBlank()) {
            token = pushDeviceTokenRepository.findByExpoPushToken(request.getExpoPushToken())
                    .filter(existing -> existing.getClient().getClientId().equals(client.getClientId()))
                    .orElse(null);
        }
        if (token == null) {
            throw new IllegalArgumentException("Push token not found for this client");
        }
        token.setStatus(TokenStatus.DISABLED);
        token.setLastSeenAt(LocalDateTime.now());
        pushDeviceTokenRepository.save(token);
    }

    public List<PushDeviceToken> getActiveTokens(String clientId) {
        return pushDeviceTokenRepository.findByClient_ClientIdAndStatus(clientId, TokenStatus.ACTIVE);
    }

    @Transactional
    public void markTokenInvalid(String expoPushToken) {
        pushDeviceTokenRepository.findByExpoPushToken(expoPushToken).ifPresent(token -> {
            token.setStatus(TokenStatus.INVALID);
            pushDeviceTokenRepository.save(token);
            log.info("Marked expo push token invalid for client {}", token.getClient().getClientId());
        });
    }

    private PushDeviceToken resolveExistingToken(String clientId, String deviceId, String expoPushToken) {
        if (deviceId != null && !deviceId.isBlank()) {
            PushDeviceToken byDevice = pushDeviceTokenRepository.findByClient_ClientIdAndDeviceId(clientId, deviceId).orElse(null);
            if (byDevice != null) {
                return byDevice;
            }
        }
        return pushDeviceTokenRepository.findByExpoPushToken(expoPushToken).orElse(null);
    }

    private String normalizeToken(String token) {
        return blankToNull(token);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
