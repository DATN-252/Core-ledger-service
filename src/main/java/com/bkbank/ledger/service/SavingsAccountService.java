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
    public SavingsAccount withdraw(String accountNumber, Double amount, String merchantId, String merchantName, String location, Double latitude, Double longitude) {
        log.info("Withdrawing {} from account {} at merchant {} (Location: {})", amount, accountNumber, merchantName, location);
        
        SavingsAccount account = getAccount(accountNumber);
        
        if (!account.isActive()) {
            Transaction failedTx = Transaction.createFailedWithdrawal(accountNumber, amount, account.getBalance(), merchantId, merchantName, location, latitude, longitude, "Account inactive");
            transactionLoggingService.logTransaction(failedTx);
            throw new RuntimeException("Account is not active");
        }
        
        if (!account.hasSufficientBalance(amount)) {
            Transaction failedTx = Transaction.createFailedWithdrawal(accountNumber, amount, account.getBalance(), merchantId, merchantName, location, latitude, longitude, "Insufficient balance");
            transactionLoggingService.logTransaction(failedTx);
            throw new RuntimeException("Insufficient balance");
        }
        
        account.withdraw(amount);
        SavingsAccount savedAccount = savingsAccountRepository.save(account);
        
        // Log transaction
        Transaction tx = Transaction.createWithdrawal(accountNumber, amount, savedAccount.getBalance(), merchantId, merchantName, location, latitude, longitude);
        transactionRepository.save(tx);
        
        log.info("Withdrawal successful. New balance: {}", savedAccount.getBalance());
        return savedAccount;
    }

    /**
     * Deposit to account
     */
    @Transactional
    public SavingsAccount deposit(String accountNumber, Double amount) {
        log.info("Depositing {} to account {}", amount, accountNumber);
        
        SavingsAccount account = getAccount(accountNumber);
        account.deposit(amount);
        SavingsAccount savedAccount = savingsAccountRepository.save(account);
        
        // Log transaction
        Transaction tx = Transaction.createDeposit(accountNumber, amount, savedAccount.getBalance());
        transactionRepository.save(tx);
        
        log.info("Deposit successful. New balance: {}", savedAccount.getBalance());
        return savedAccount;
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
