package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Page<Transaction> findByAccountNumberOrderByTransactionDateDesc(String accountNumber, Pageable pageable);
    
    Page<Transaction> findByAccountNumberInOrderByTransactionDateDesc(List<String> accountNumbers, Pageable pageable);
    
    List<Transaction> findTop10ByAccountNumberOrderByTransactionDateDesc(String accountNumber);
}
