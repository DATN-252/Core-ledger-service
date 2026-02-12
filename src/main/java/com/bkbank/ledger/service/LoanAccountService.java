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
    public LoanAccount addCharge(String accountNumber, Double amount) {
        log.info("Adding charge of {} to loan account {}", amount, accountNumber);
        
        LoanAccount account = getAccount(accountNumber);
        
        if (!account.isActive()) {
            throw new RuntimeException("Loan account is not active");
        }
        
        if (!account.hasSufficientCredit(amount)) {
            throw new RuntimeException("Credit limit exceeded");
        }
        
        account.addCharge(amount);
        LoanAccount savedAccount = loanAccountRepository.save(account);
        
        // Log transaction
        Transaction tx = Transaction.createCharge(accountNumber, amount, savedAccount.getPrincipalOutstanding());
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
        account.setClient(client);  // Set client reference
        account.setCurrency("USD");
        account.setStatus("ACTIVE");
        
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
}
