package com.bkbank.ledger.service;

import com.bkbank.ledger.client.CmsClient;
import com.bkbank.ledger.entity.FraudAlertEmailAction;
import com.bkbank.ledger.entity.enums.FraudAlertEmailActionDecision;
import com.bkbank.ledger.entity.enums.FraudAlertEmailActionStatus;
import com.bkbank.ledger.repository.FraudAlertEmailActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAlertEmailActionService {

    private final FraudAlertEmailActionRepository fraudAlertEmailActionRepository;
    private final CmsClient cmsClient;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.public-base-url:http://localhost:8083}")
    private String publicBaseUrl;

    @Value("${fraud.email-action.expiration-minutes:15}")
    private long expirationMinutes;

    @Transactional
    public EmailActionLinks createLinks(Long fraudAlertId,
                                        String clientId,
                                        String accountId,
                                        String paymentId,
                                        String maskedPan,
                                        String merchantName,
                                        Double amount,
                                        String currency,
                                        String riskLevel) {
        cancelActiveTokens(fraudAlertId);

        String confirmToken = createTokenRecord(
                fraudAlertId, clientId, accountId, paymentId, maskedPan, merchantName, amount, currency, riskLevel,
                FraudAlertEmailActionDecision.CONFIRM
        );
        String rejectToken = createTokenRecord(
                fraudAlertId, clientId, accountId, paymentId, maskedPan, merchantName, amount, currency, riskLevel,
                FraudAlertEmailActionDecision.REJECT
        );

        return new EmailActionLinks(buildUrl(confirmToken), buildUrl(rejectToken));
    }

    @Transactional(readOnly = true)
    public EmailActionPreview getPreview(String rawToken) {
        FraudAlertEmailAction action = requireUsableToken(rawToken);
        Map<String, Object> alert = cmsClient.getFraudAlertDetail(action.getFraudAlertId());
        return new EmailActionPreview(action, alert);
    }

    @Transactional
    public EmailActionExecutionResult execute(String rawToken) {
        FraudAlertEmailAction action = requireUsableToken(rawToken);
        String note = action.getDecision() == FraudAlertEmailActionDecision.CONFIRM
                ? "Confirmed via email link"
                : "Rejected via email link";

        Map<String, Object> response = action.getDecision() == FraudAlertEmailActionDecision.CONFIRM
                ? cmsClient.confirmFraudAlert(action.getFraudAlertId(), note)
                : cmsClient.rejectFraudAlert(action.getFraudAlertId(), note);

        if (response == null) {
            throw new IllegalStateException("Khong the xu ly fraud alert luc nay");
        }

        action.setStatus(FraudAlertEmailActionStatus.USED);
        action.setUsedAt(LocalDateTime.now());
        fraudAlertEmailActionRepository.save(action);
        cancelSiblingTokens(action);

        return new EmailActionExecutionResult(action, response);
    }

    private String createTokenRecord(Long fraudAlertId,
                                     String clientId,
                                     String accountId,
                                     String paymentId,
                                     String maskedPan,
                                     String merchantName,
                                     Double amount,
                                     String currency,
                                     String riskLevel,
                                     FraudAlertEmailActionDecision decision) {
        String rawToken = generateRawToken();
        FraudAlertEmailAction action = new FraudAlertEmailAction();
        action.setFraudAlertId(fraudAlertId);
        action.setClientId(clientId);
        action.setAccountId(accountId);
        action.setDecision(decision);
        action.setTokenHash(hashToken(rawToken));
        action.setStatus(FraudAlertEmailActionStatus.ACTIVE);
        action.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        action.setPaymentId(paymentId);
        action.setMaskedPan(maskedPan);
        action.setMerchantName(merchantName);
        action.setAmount(amount);
        action.setCurrency(currency);
        action.setRiskLevel(riskLevel);
        fraudAlertEmailActionRepository.save(action);
        return rawToken;
    }

    private String buildUrl(String rawToken) {
        return publicBaseUrl.replaceAll("/+$", "") + "/public/fraud-alert-actions/" + rawToken;
    }

    private void cancelActiveTokens(Long fraudAlertId) {
        List<FraudAlertEmailAction> activeTokens = fraudAlertEmailActionRepository
                .findByFraudAlertIdAndStatus(fraudAlertId, FraudAlertEmailActionStatus.ACTIVE);
        if (activeTokens.isEmpty()) {
            return;
        }
        activeTokens.forEach(token -> token.setStatus(FraudAlertEmailActionStatus.CANCELLED));
        fraudAlertEmailActionRepository.saveAll(activeTokens);
    }

    private void cancelSiblingTokens(FraudAlertEmailAction usedAction) {
        List<FraudAlertEmailAction> activeTokens = fraudAlertEmailActionRepository
                .findByFraudAlertIdAndStatus(usedAction.getFraudAlertId(), FraudAlertEmailActionStatus.ACTIVE);
        if (activeTokens.isEmpty()) {
            return;
        }
        activeTokens.forEach(token -> token.setStatus(FraudAlertEmailActionStatus.CANCELLED));
        fraudAlertEmailActionRepository.saveAll(activeTokens);
    }

    private FraudAlertEmailAction requireUsableToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token khong hop le");
        }
        FraudAlertEmailAction action = fraudAlertEmailActionRepository.findByTokenHash(hashToken(rawToken.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Lien ket khong hop le hoac da bi thu hoi"));

        if (action.getStatus() == FraudAlertEmailActionStatus.USED) {
            throw new IllegalStateException("Lien ket nay da duoc su dung");
        }
        if (action.getStatus() == FraudAlertEmailActionStatus.CANCELLED) {
            throw new IllegalStateException("Lien ket nay khong con hieu luc");
        }
        if (action.getStatus() == FraudAlertEmailActionStatus.EXPIRED || LocalDateTime.now().isAfter(action.getExpiresAt())) {
            if (action.getStatus() != FraudAlertEmailActionStatus.EXPIRED) {
                action.setStatus(FraudAlertEmailActionStatus.EXPIRED);
                fraudAlertEmailActionRepository.save(action);
            }
            throw new IllegalStateException("Lien ket nay da het han");
        }
        return action;
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Khong the tao hash token", e);
        }
    }

    public record EmailActionLinks(String confirmUrl, String rejectUrl) {
    }

    public record EmailActionPreview(FraudAlertEmailAction action, Map<String, Object> alert) {
    }

    public record EmailActionExecutionResult(FraudAlertEmailAction action, Map<String, Object> response) {
    }
}
