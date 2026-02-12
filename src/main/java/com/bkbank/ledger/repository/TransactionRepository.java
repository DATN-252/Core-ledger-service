package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByAccountNumberOrderByTransactionDateDesc(String accountNumber);
    
    List<Transaction> findTop10ByAccountNumberOrderByTransactionDateDesc(String accountNumber);
}
