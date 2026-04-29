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

    @Transactional
    public Transaction applyStatementInterest(String accountNumber,
                                              Double amount,
                                              String billingDate,
                                              Double interestRateMonthly) {
        log.info("Applying statement interest of {} to loan account {} for billing date {}", amount, accountNumber, billingDate);

        LoanAccount account = getAccount(accountNumber);
        account.applyStatementInterest(amount);
        LoanAccount savedAccount = loanAccountRepository.save(account);

        Transaction tx = Transaction.createCharge(accountNumber, amount, savedAccount.getCurrency(), savedAccount.getPrincipalOutstanding(), null, null, null, null, null);
        tx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        tx.setChannel("STATEMENT_INTEREST");
        tx.setTransactionType("INTEREST");
        tx.setDescription("Statement interest"
                + (billingDate != null && !billingDate.isBlank() ? " for " + billingDate : "")
                + (interestRateMonthly != null ? " at " + interestRateMonthly + "% monthly" : ""));
        tx.setExternalReference(billingDate);
        tx.setResponseCode("00");
        tx.setResponseMessage("Approved");
        Transaction savedTx = transactionRepository.save(tx);
        transactionNotificationEventPublisher.publish(savedTx.getId());

        log.info("Statement interest applied. New outstanding: {}", savedAccount.getPrincipalOutstanding());
        return savedTx;
    }

    @Transactional
    public Transaction applyStatementLateFee(String accountNumber,
                                             Double amount,
                                             String billingDate) {
        log.info("Applying statement late fee of {} to loan account {} for billing date {}", amount, accountNumber, billingDate);

        LoanAccount account = getAccount(accountNumber);
        account.applyStatementLateFee(amount);
        LoanAccount savedAccount = loanAccountRepository.save(account);

        Transaction tx = Transaction.createCharge(accountNumber, amount, savedAccount.getCurrency(), savedAccount.getPrincipalOutstanding(), null, null, null, null, null);
        tx.assignBranch(savedAccount.getBranchId(), savedAccount.getBranchName());
        tx.setChannel("STATEMENT_LATE_FEE");
        tx.setTransactionType("LATE_FEE");
        tx.setDescription("Statement late fee"
                + (billingDate != null && !billingDate.isBlank() ? " for " + billingDate : ""));
        tx.setExternalReference(billingDate);
        tx.setResponseCode("00");
        tx.setResponseMessage("Approved");
        Transaction savedTx = transactionRepository.save(tx);
        transactionNotificationEventPublisher.publish(savedTx.getId());

        log.info("Statement late fee applied. New outstanding: {}", savedAccount.getPrincipalOutstanding());
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
        account.setStatementInterestRateMonthly(2.5);
        account.setStatementLateFeeFixed(15.0);
        account.setStatus(com.bkbank.ledger.entity.enums.AccountStatus.PENDING);
        
        LoanAccount savedAccount = loanAccountRepository.save(account);
        log.info("Created loan account: {} with principal {} for client: {}", 
                accountNumber, principal, client.getFullName());
        
        return savedAccount;
    }

    @Transactional
    public LoanAccount updateStatementSettings(String accountNumber,
                                               Integer billingDayOfMonth,
                                               Integer paymentDueDays,
                                               Double minimumPaymentRate,
                                               Double minimumPaymentFloor,
                                               Double statementInterestRateMonthly,
                                               Double statementLateFeeFixed) {
        LoanAccount account = getAccount(accountNumber);

        validateStatementSettings(
                billingDayOfMonth,
                paymentDueDays,
                minimumPaymentRate,
                minimumPaymentFloor,
                statementInterestRateMonthly,
                statementLateFeeFixed
        );

        account.setBillingDayOfMonth(billingDayOfMonth);
        account.setPaymentDueDays(paymentDueDays);
        account.setMinimumPaymentRate(minimumPaymentRate);
        account.setMinimumPaymentFloor(minimumPaymentFloor);
        account.setStatementInterestRateMonthly(statementInterestRateMonthly);
        account.setStatementLateFeeFixed(statementLateFeeFixed);

        return loanAccountRepository.save(account);
    }

    /**
     * Get transaction history
     */
    public List<Transaction> getTransactionHistory(String accountNumber) {
        return transactionRepository.findByAccountNumberOrderByTransactionDateDesc(accountNumber, Pageable.unpaged()).getContent();
    }

    private void validateStatementSettings(Integer billingDayOfMonth,
                                           Integer paymentDueDays,
                                           Double minimumPaymentRate,
                                           Double minimumPaymentFloor,
                                           Double statementInterestRateMonthly,
                                           Double statementLateFeeFixed) {
        if (billingDayOfMonth == null || billingDayOfMonth < 1 || billingDayOfMonth > 28) {
            throw new IllegalArgumentException("billingDayOfMonth must be between 1 and 28");
        }
        if (paymentDueDays == null || paymentDueDays < 1 || paymentDueDays > 60) {
            throw new IllegalArgumentException("paymentDueDays must be between 1 and 60");
        }
        if (minimumPaymentRate == null || minimumPaymentRate < 0 || minimumPaymentRate > 100) {
            throw new IllegalArgumentException("minimumPaymentRate must be between 0 and 100");
        }
        if (minimumPaymentFloor == null || minimumPaymentFloor < 0) {
            throw new IllegalArgumentException("minimumPaymentFloor must be greater than or equal to 0");
        }
        if (statementInterestRateMonthly == null || statementInterestRateMonthly < 0 || statementInterestRateMonthly > 100) {
            throw new IllegalArgumentException("statementInterestRateMonthly must be between 0 and 100");
        }
        if (statementLateFeeFixed == null || statementLateFeeFixed < 0) {
            throw new IllegalArgumentException("statementLateFeeFixed must be greater than or equal to 0");
        }
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
