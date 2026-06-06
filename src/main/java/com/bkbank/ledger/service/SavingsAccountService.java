package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.ClientRepository;
import com.bkbank.ledger.repository.SavingsAccountRepository;
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
public class SavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLoggingService transactionLoggingService;
    private final TransactionNotificationEventPublisher transactionNotificationEventPublisher;

    /**
     * Get account by account number
     */
    public SavingsAccount getAccount(String accountNumber) {
        return savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Savings account not found: " + accountNumber));
    }

    /**
     * Get account balance
     */
    public Double getBalance(String accountNumber) {
        SavingsAccount account = getAccount(accountNumber);
        log.info("Account {} balance: {}", accountNumber, account.getBalance());
        return account.getBalance();
    }

    /**
     * Withdraw from account (for debit card transactions)
     */
    @Transactional
    public SavingsAccount withdraw(String accountNumber, Double amount, String merchantId, String merchantName, String location, Double latitude, Double longitude,
                                   String paymentId, String idempotencyKey, String originalTransactionId, String channel,
                                   String authCode, String stan, String rrn, String externalReference,
                                   String responseCode, String responseMessage, String paymentNote) {
        log.info("Withdrawing {} from account {} at merchant {} (Location: {})", amount, accountNumber, merchantName, location);
        
        SavingsAccount account = getAccount(accountNumber);
        
        if (!account.isActive()) {
            Transaction failedTx = Transaction.createFailedWithdrawal(accountNumber, amount, account.getCurrency(), account.getBalance(), merchantId, merchantName, location, latitude, longitude, "Account inactive");
            failedTx.assignBranch(account.getBranchId(), account.getBranchName());
            failedTx.applyReferenceData(paymentId, idempotencyKey, originalTransactionId, channel, authCode, stan, rrn, externalReference,
                    responseCode != null ? responseCode : "96",
                    responseMessage != null ? responseMessage : "Account inactive");
            applyPaymentNote(failedTx, paymentNote);
            transactionLoggingService.logTransaction(failedTx);
            throw new RuntimeException("Account is not active");
        }
        
        if (!account.hasSufficientBalance(amount)) {
            Transaction failedTx = Transaction.createFailedWithdrawal(accountNumber, amount, account.getCurrency(), account.getBalance(), merchantId, merchantName, location, latitude, longitude, "Insufficient balance");
            failedTx.assignBranch(account.getBranchId(), account.getBranchName());
            failedTx.applyReferenceData(paymentId, idempotencyKey, originalTransactionId, channel, authCode, stan, rrn, externalReference,
                    responseCode != null ? responseCode : "51",
                    responseMessage != null ? responseMessage : "Insufficient balance");
            applyPaymentNote(failedTx, paymentNote);
            transactionLoggingService.logTransaction(failedTx);
            throw new RuntimeException("Insufficient balance");
        }
        
        account.withdraw(amount);
        SavingsAccount savedAccount = savingsAccountRepository.save(account);
        
        // Log transaction
        Transaction tx = Transaction.createWithdrawal(accountNumber, amount, savedAccount.getCurrency(), savedAccount.getBalance(), merchantId, merchantName, location, latitude, longitude);
        tx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        tx.applyReferenceData(paymentId, idempotencyKey, originalTransactionId, channel, authCode, stan, rrn, externalReference,
                responseCode != null ? responseCode : "00",
                responseMessage != null ? responseMessage : "Approved");
        applyPaymentNote(tx, paymentNote);
        Transaction savedTx = transactionRepository.save(tx);
        transactionNotificationEventPublisher.publish(savedTx.getId());
        
        log.info("Withdrawal successful. New balance: {}", savedAccount.getBalance());
        return savedAccount;
    }

    /**
     * Deposit to account
     */
    @Transactional
    public SavingsAccount deposit(String accountNumber, Double amount) {
        log.info("Depositing {} to account {}", amount, accountNumber);

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0");
        }
        
        SavingsAccount account = getAccount(accountNumber);
        account.deposit(amount);
        SavingsAccount savedAccount = savingsAccountRepository.save(account);
        
        // Log transaction
        Transaction tx = Transaction.createDeposit(accountNumber, amount, savedAccount.getCurrency(), savedAccount.getBalance());
        tx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        Transaction savedTx = transactionRepository.save(tx);
        transactionNotificationEventPublisher.publish(savedTx.getId());
        
        log.info("Deposit successful. New balance: {}", savedAccount.getBalance());
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
    public SavingsAccount withdrawStatementPayment(String accountNumber,
                                                   Double amount,
                                                   String loanAccountNumber,
                                                   String billingDate,
                                                   String note) {
        log.info("Withdrawing {} from savings account {} for statement payment to {}", amount, accountNumber, loanAccountNumber);

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        SavingsAccount account = getAccount(accountNumber);

        if (!account.isActive()) {
            throw new RuntimeException("Source savings account is not active");
        }

        if (!account.hasSufficientBalance(amount)) {
            throw new RuntimeException("Insufficient balance");
        }

        account.withdraw(amount);
        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        Transaction tx = Transaction.createWithdrawal(
                accountNumber,
                amount,
                savedAccount.getCurrency(),
                savedAccount.getBalance(),
                loanAccountNumber,
                "BKBank Credit Card",
                "Statement " + billingDate,
                null,
                null
        );
        tx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        tx.setDescription("Statement payment"
                + (loanAccountNumber != null && !loanAccountNumber.isBlank() ? " to " + loanAccountNumber : "")
                + (billingDate != null && !billingDate.isBlank() ? " for " + billingDate : "")
                + (note != null && !note.isBlank() ? " - " + note : ""));
        tx.setChannel("STATEMENT_PAYMENT");
        tx.setResponseCode("00");
        tx.setResponseMessage("Approved");
        Transaction savedTx = transactionRepository.save(tx);
        transactionNotificationEventPublisher.publish(savedTx.getId());

        return savedAccount;
    }

    @Transactional
    public SavingsAccount depositSettlement(String accountNumber,
                                            Double amount,
                                            String merchantId,
                                            String merchantName,
                                            String settlementReference,
                                            String note) {
        log.info("Depositing settlement {} to account {} for merchant {}", amount, accountNumber, merchantId);

        SavingsAccount account = getAccount(accountNumber);
        account.deposit(amount);
        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        Transaction tx = Transaction.createSettlementDeposit(
                accountNumber,
                amount,
                savedAccount.getCurrency(),
                savedAccount.getBalance(),
                merchantId,
                merchantName,
                settlementReference,
                note
        );
        tx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        Transaction savedTx = transactionRepository.save(tx);
        transactionNotificationEventPublisher.publish(savedTx.getId());

        return savedAccount;
    }

    /**
     * Apply a refund/reversal credit to a debit account and log it as a dedicated transaction type.
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
        SavingsAccount account = getAccount(accountNumber);
        account.applyCardAdjustment(originalTransaction.getAmount());
        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        Transaction adjustmentTx = "REVERSAL".equalsIgnoreCase(adjustmentType)
                ? Transaction.createReversal(originalTransaction, savedAccount.getBalance(), reason)
                : Transaction.createRefund(originalTransaction, savedAccount.getBalance(), reason);
        adjustmentTx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
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
     * Create new savings account
     */
    @Transactional
    public SavingsAccount createAccount(String accountNumber, Double initialBalance, String clientId) {
        if (savingsAccountRepository.existsByAccountNumber(accountNumber)) {
            throw new RuntimeException("Account number already exists: " + accountNumber);
        }
        
        // Lookup client
        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));
        
        SavingsAccount account = new SavingsAccount();
        account.setAccountNumber(accountNumber);
        account.setBalance(initialBalance != null ? initialBalance : 0.0);
        account.setClient(client);
        account.setBranch(client.getHomeBranch());
        account.setCurrency("USD");
        
        SavingsAccount savedAccount = savingsAccountRepository.save(account);
        log.info("Created savings account: {} for client: {}", accountNumber, client.getFullName());
        
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
    public SavingsAccount activateAccount(String accountNumber) {
        log.info("Activating savings account: {}", accountNumber);
        SavingsAccount account = getAccount(accountNumber);
        account.activate();
        return savingsAccountRepository.save(account);
    }

    /**
     * Lock account (ACTIVE → LOCKED)
     */
    @Transactional
    public SavingsAccount lockAccount(String accountNumber, String reason) {
        log.info("Locking savings account: {} - Reason: {}", accountNumber, reason);
        SavingsAccount account = getAccount(accountNumber);
        account.lock(reason);
        return savingsAccountRepository.save(account);
    }

    /**
     * Unlock account (LOCKED → ACTIVE)
     */
    @Transactional
    public SavingsAccount unlockAccount(String accountNumber) {
        log.info("Unlocking savings account: {}", accountNumber);
        SavingsAccount account = getAccount(accountNumber);
        account.unlock();
        return savingsAccountRepository.save(account);
    }

    /**
     * Close account (ACTIVE/LOCKED → CLOSED)
     */
    @Transactional
    public SavingsAccount closeAccount(String accountNumber) {
        log.info("Closing savings account: {}", accountNumber);
        SavingsAccount account = getAccount(accountNumber);
        account.close();
        return savingsAccountRepository.save(account);
    }
}
