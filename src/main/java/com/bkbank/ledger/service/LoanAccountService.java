package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.ClientRepository;
import com.bkbank.ledger.repository.LoanAccountRepository;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanAccountService {

    private final LoanAccountRepository loanAccountRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLoggingService transactionLoggingService;
    private final TransactionNotificationEventPublisher transactionNotificationEventPublisher;

    /**
     * Get loan account by account number
     */
    public LoanAccount getAccount(String accountNumber) {
        return loanAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Loan account not found: " + accountNumber));
    }

    /**
     * Get available credit (for credit card transactions)
     */
    public Double getAvailableCredit(String accountNumber) {
        LoanAccount account = getAccount(accountNumber);
        Double available = account.getAvailableCredit();
        log.info("Account {} available credit: {} (Limit: {}, Outstanding: {})", 
            accountNumber, available, account.getPrincipal(), account.getPrincipalOutstanding());
        return available;
    }

    /**
     * Add charge to loan (for credit card transactions)
     */
    @Transactional
    public LoanAccount addCharge(String accountNumber, Double amount, String merchantId, String merchantName, String cardNetwork,
                                 String location, Double latitude, Double longitude, String paymentId, String idempotencyKey,
                                 String originalTransactionId, String channel, String authCode, String stan, String rrn,
                                 String externalReference, String responseCode, String responseMessage, String paymentNote) {
        log.info("Adding charge of {} to loan account {} at merchant {} with network {} (Location: {})", amount, accountNumber, merchantName, cardNetwork, location);
        
        LoanAccount account = getAccount(accountNumber);
        
        if (!account.isActive()) {
            Transaction failedTx = Transaction.createFailedCharge(accountNumber, amount, account.getCurrency(), account.getPrincipalOutstanding(), merchantId, merchantName, location, latitude, longitude, "Account inactive");
            failedTx.assignBranch(account.getBranchId(), account.getBranchName());
            failedTx.setCardNetwork(cardNetwork);
            failedTx.applyReferenceData(paymentId, idempotencyKey, originalTransactionId, channel, authCode, stan, rrn, externalReference,
                    responseCode != null ? responseCode : "96",
                    responseMessage != null ? responseMessage : "Account inactive");
            applyPaymentNote(failedTx, paymentNote);
            transactionLoggingService.logTransaction(failedTx);
            throw new RuntimeException("Loan account is not active");
        }
        
        if (!account.hasSufficientCredit(amount)) {
            Transaction failedTx = Transaction.createFailedCharge(accountNumber, amount, account.getCurrency(), account.getPrincipalOutstanding(), merchantId, merchantName, location, latitude, longitude, "Credit limit exceeded");
            failedTx.assignBranch(account.getBranchId(), account.getBranchName());
            failedTx.setCardNetwork(cardNetwork);
            failedTx.applyReferenceData(paymentId, idempotencyKey, originalTransactionId, channel, authCode, stan, rrn, externalReference,
                    responseCode != null ? responseCode : "51",
                    responseMessage != null ? responseMessage : "Credit limit exceeded");
            applyPaymentNote(failedTx, paymentNote);
            transactionLoggingService.logTransaction(failedTx);
            throw new RuntimeException("Credit limit exceeded");
        }
        
        account.addCharge(amount);
        LoanAccount savedAccount = loanAccountRepository.save(account);
        
        // Log transaction
        Transaction tx = Transaction.createCharge(accountNumber, amount, savedAccount.getCurrency(), savedAccount.getPrincipalOutstanding(), merchantId, merchantName, location, latitude, longitude);
        tx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        tx.setCardNetwork(cardNetwork);
        tx.applyReferenceData(paymentId, idempotencyKey, originalTransactionId, channel, authCode, stan, rrn, externalReference,
                responseCode != null ? responseCode : "00",
                responseMessage != null ? responseMessage : "Approved");
        applyPaymentNote(tx, paymentNote);
        Transaction savedTx = transactionRepository.save(tx);
        transactionNotificationEventPublisher.publish(savedTx.getId());
        
        log.info("Charge added successfully. New outstanding: {}", savedAccount.getPrincipalOutstanding());
        return savedAccount;
    }

    /**
     * Make payment to loan
     */
    @Transactional
    public LoanAccount makePayment(String accountNumber, Double amount) {
        log.info("Processing payment of {} to loan account {}", amount, accountNumber);
        
        LoanAccount account = getAccount(accountNumber);
        account.makePayment(amount);
        LoanAccount savedAccount = loanAccountRepository.save(account);
        
        // Log transaction
        Transaction tx = Transaction.createPayment(accountNumber, amount, savedAccount.getCurrency(), savedAccount.getPrincipalOutstanding());
        tx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        Transaction savedTx = transactionRepository.save(tx);
        transactionNotificationEventPublisher.publish(savedTx.getId());
        
        log.info("Payment processed. New outstanding: {}", savedAccount.getPrincipalOutstanding());
        return savedAccount;
    }

    private void applyPaymentNote(Transaction tx, String paymentNote) {
        if (paymentNote == null || paymentNote.isBlank()) {
            return;
        }
        String trimmed = paymentNote.trim();
        String baseDescription = tx.getDescription();
        if (baseDescription == null || baseDescription.isBlank()) {
            tx.setDescription(trimmed);
            return;
        }
        tx.setDescription(baseDescription + " - " + trimmed);
    }

    @Transactional
    public Transaction makeStatementPayment(String accountNumber,
                                            Double amount,
                                            String billingDate,
                                            String paymentSource,
                                            String sourceAccountNumber,
                                            String note) {
        log.info("Processing statement payment of {} to loan account {} for billing date {}", amount, accountNumber, billingDate);

        LoanAccount account = getAccount(accountNumber);
        account.makePayment(amount);
        LoanAccount savedAccount = loanAccountRepository.save(account);

        Transaction tx = Transaction.createPayment(accountNumber, amount, savedAccount.getCurrency(), savedAccount.getPrincipalOutstanding());
        tx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        tx.setChannel("STATEMENT_PAYMENT");
        tx.setDescription("Statement payment"
                + (billingDate != null && !billingDate.isBlank() ? " for " + billingDate : "")
                + (paymentSource != null && !paymentSource.isBlank() ? " via " + paymentSource : "")
                + (sourceAccountNumber != null && !sourceAccountNumber.isBlank() ? " from " + sourceAccountNumber : "")
                + (note != null && !note.isBlank() ? " - " + note : ""));
        tx.setExternalReference(billingDate);
        Transaction savedTx = transactionRepository.save(tx);
        transactionNotificationEventPublisher.publish(savedTx.getId());

        log.info("Statement payment processed. New outstanding: {}", savedAccount.getPrincipalOutstanding());
        return savedTx;
    }

    /**
     * Apply a refund/reversal against a credit card charge and log it as a dedicated transaction type.
     */
    @Transactional
    public Transaction applyCardAdjustment(String accountNumber,
                                           Transaction originalTransaction,
                                           String adjustmentType,
                                           String reason,
                                           String paymentId,
                                           String idempotencyKey,
                                           String channel,
                                           String externalReference) {
        LoanAccount account = getAccount(accountNumber);
        account.applyCardAdjustment(originalTransaction.getAmount());
        LoanAccount savedAccount = loanAccountRepository.save(account);

        Transaction adjustmentTx = "REVERSAL".equalsIgnoreCase(adjustmentType)
                ? Transaction.createReversal(originalTransaction, savedAccount.getPrincipalOutstanding(), reason)
                : Transaction.createRefund(originalTransaction, savedAccount.getPrincipalOutstanding(), reason);
        adjustmentTx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        adjustmentTx.setCardNetwork(originalTransaction.getCardNetwork());
        adjustmentTx.applyReferenceData(
                paymentId,
                idempotencyKey,
                originalTransaction.getPaymentId(),
                channel,
                null,
                null,
                null,
                externalReference,
                "00",
                reason != null && !reason.isBlank() ? reason : adjustmentType + " successful"
        );
        Transaction savedTx = transactionRepository.save(adjustmentTx);
        transactionNotificationEventPublisher.publish(savedTx.getId());
        return savedTx;
    }

    /**
     * Create new loan account (for credit cards)
     */
    @Transactional
    public LoanAccount createAccount(String accountNumber, Double principal, String clientId) {
        if (loanAccountRepository.existsByAccountNumber(accountNumber)) {
            throw new RuntimeException("Account number already exists: " + accountNumber);
        }
        
        // Lookup client
        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));
        
        LoanAccount account = new LoanAccount();
        account.setAccountNumber(accountNumber);
        account.setPrincipal(principal);
        account.setPrincipalOutstanding(0.0);
        account.setClient(client);
        account.setBranch(client.getHomeBranch());
        account.setCurrency("USD");
        account.setStatus(com.bkbank.ledger.entity.enums.AccountStatus.PENDING);
        
        LoanAccount savedAccount = loanAccountRepository.save(account);
        log.info("Created loan account: {} with principal {} for client: {}", 
                accountNumber, principal, client.getFullName());
        
        return savedAccount;
    }

    /**
     * Get transaction history
     */
    public List<Transaction> getTransactionHistory(String accountNumber) {
        return transactionRepository.findByAccountNumberOrderByTransactionDateDesc(accountNumber, Pageable.unpaged()).getContent();
    }

    /**
     * Activate account (PENDING → ACTIVE)
     */
    @Transactional
    public LoanAccount activateAccount(String accountNumber) {
        log.info("Activating loan account: {}", accountNumber);
        LoanAccount account = getAccount(accountNumber);
        account.activate();
        return loanAccountRepository.save(account);
    }

    /**
     * Lock account (ACTIVE → LOCKED)
     */
    @Transactional
    public LoanAccount lockAccount(String accountNumber, String reason) {
        log.info("Locking loan account: {} - Reason: {}", accountNumber, reason);
        LoanAccount account = getAccount(accountNumber);
        account.lock(reason);
        return loanAccountRepository.save(account);
    }

    /**
     * Unlock account (LOCKED → ACTIVE)
     */
    @Transactional
    public LoanAccount unlockAccount(String accountNumber) {
        log.info("Unlocking loan account: {}", accountNumber);
        LoanAccount account = getAccount(accountNumber);
        account.unlock();
        return loanAccountRepository.save(account);
    }

    /**
     * Close account (ACTIVE/LOCKED → CLOSED)
     */
    @Transactional
    public LoanAccount closeAccount(String accountNumber) {
        log.info("Closing loan account: {}", accountNumber);
        LoanAccount account = getAccount(accountNumber);
        account.close();
        return loanAccountRepository.save(account);
    }
}
