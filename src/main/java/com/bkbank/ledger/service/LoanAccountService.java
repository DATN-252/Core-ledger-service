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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanAccountService {

    private final LoanAccountRepository loanAccountRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLoggingService transactionLoggingService;

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
    public LoanAccount addCharge(String accountNumber, Double amount, String merchantId, String merchantName, String cardNetwork) {
        log.info("Adding charge of {} to loan account {} at merchant {} with network {}", amount, accountNumber, merchantName, cardNetwork);
        
        LoanAccount account = getAccount(accountNumber);
        
        if (!account.isActive()) {
            Transaction failedTx = Transaction.createFailedCharge(accountNumber, amount, account.getPrincipalOutstanding(), merchantId, merchantName, "Account inactive");
            failedTx.setCardNetwork(cardNetwork);
            transactionLoggingService.logTransaction(failedTx);
            throw new RuntimeException("Loan account is not active");
        }
        
        if (!account.hasSufficientCredit(amount)) {
            Transaction failedTx = Transaction.createFailedCharge(accountNumber, amount, account.getPrincipalOutstanding(), merchantId, merchantName, "Credit limit exceeded");
            failedTx.setCardNetwork(cardNetwork);
            transactionLoggingService.logTransaction(failedTx);
            throw new RuntimeException("Credit limit exceeded");
        }
        
        account.addCharge(amount);
        LoanAccount savedAccount = loanAccountRepository.save(account);
        
        // Log transaction
        Transaction tx = Transaction.createCharge(accountNumber, amount, savedAccount.getPrincipalOutstanding(), merchantId, merchantName);
        tx.setCardNetwork(cardNetwork);
        transactionRepository.save(tx);
        
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
        Transaction tx = Transaction.createPayment(accountNumber, amount, savedAccount.getPrincipalOutstanding());
        transactionRepository.save(tx);
        
        log.info("Payment processed. New outstanding: {}", savedAccount.getPrincipalOutstanding());
        return savedAccount;
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
        return transactionRepository.findByAccountNumberOrderByTransactionDateDesc(accountNumber);
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
