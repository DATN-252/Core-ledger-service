package com.bkbank.ledger.entity.enums;

/**
 * Account Status Enum
 * Defines valid states for savings and loan accounts
 */
public enum AccountStatus {
    /**
     * Account newly created, not yet activated
     */
    PENDING,
    
    /**
     * Account is active and can be used for transactions
     */
    ACTIVE,
    
    /**
     * Account is temporarily locked (fraud, customer request)
     * Loan accounts can still receive payments when locked
     */
    LOCKED,
    
    /**
     * Account is permanently closed
     * This is a final state - cannot be reopened
     */
    CLOSED
}
