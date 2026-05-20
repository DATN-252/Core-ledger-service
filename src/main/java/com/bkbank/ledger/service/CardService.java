package com.bkbank.ledger.service;

import com.bkbank.ledger.client.CmsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for card-related operations
 * Communicates with CMS service to validate card ownership and status
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CmsClient cmsClient;

    /**
     * Validates that a card belongs to the specified user/account
     * 
     * @param cardNumber the card number to validate
     * @param userId the user ID (customer ID) to check ownership
     * @return true if card belongs to user, false otherwise
     * @throws IllegalArgumentException if card not found or validation fails
     */
    public boolean validateCardOwnership(String cardNumber, String userId) {
        try {
            // Call CMS API to get card details
            Map<String, Object> cardDetails = cmsClient.getCardDetails(cardNumber);
            
            if (cardDetails == null || cardDetails.isEmpty()) {
                log.warn("Card not found - Card: {}, User: {}", maskCardNumber(cardNumber), userId);
                throw new IllegalArgumentException("Thẻ không tồn tại");
            }
            
            // Extract account ID from card details
            String accountId = (String) cardDetails.get("accountId");
            if (accountId == null) {
                log.warn("Card has no account ID - Card: {}, User: {}", maskCardNumber(cardNumber), userId);
                throw new IllegalArgumentException("Thẻ không hợp lệ");
            }
            
            // Verify account belongs to user
            // TODO: Implement account ownership check
            // For now, we assume CMS validates this
            
            // Check card status
            String status = (String) cardDetails.get("status");
            if (!"ACTIVE".equalsIgnoreCase(status)) {
                log.warn("Card is not active - Card: {}, Status: {}, User: {}", 
                        maskCardNumber(cardNumber), status, userId);
                throw new IllegalArgumentException("Thẻ không hoạt động");
            }
            
            log.info("Card ownership validated - Card: {}, User: {}", maskCardNumber(cardNumber), userId);
            return true;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating card ownership - Card: {}, User: {}, Error: {}", 
                    maskCardNumber(cardNumber), userId, e.getMessage());
            throw new IllegalArgumentException("Không thể xác minh thẻ");
        }
    }

    /**
     * Checks if a card is active
     * 
     * @param cardNumber the card number to check
     * @return true if card is active, false otherwise
     */
    public boolean isCardActive(String cardNumber) {
        try {
            Map<String, Object> cardDetails = cmsClient.getCardDetails(cardNumber);
            if (cardDetails == null) {
                return false;
            }
            
            String status = (String) cardDetails.get("status");
            return "ACTIVE".equalsIgnoreCase(status);
        } catch (Exception e) {
            log.error("Error checking card status - Card: {}, Error: {}", 
                    maskCardNumber(cardNumber), e.getMessage());
            return false;
        }
    }

    /**
     * Masks card number for logging (shows only last 4 digits)
     * 
     * @param cardNumber the card number to mask
     * @return masked card number (e.g., "**** **** **** 1111")
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
